from __future__ import annotations

from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from app.schemas import AnalysisResponse
from app.services.audio import extract_audio_features
from app.services.inference import RespiratoryModelService
from app.services.reporting import build_diagnostic_report


BASE_DIR = Path(__file__).resolve().parent.parent
STATIC_DIR = BASE_DIR / "static"

app = FastAPI(
    title="Respiratory Disease Screening MVP",
    description="Upload breathing sounds, extract features, and generate AI-assisted screening outputs.",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

model_service = RespiratoryModelService()


@app.get("/", include_in_schema=False)
async def index() -> FileResponse:
    return FileResponse(STATIC_DIR / "index.html")


@app.get("/api/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/analyze", response_model=AnalysisResponse)
async def analyze_audio(file: UploadFile = File(...)) -> AnalysisResponse:
    if not file.filename:
        raise HTTPException(status_code=400, detail="A file name is required.")

    try:
        content = await file.read()
        features = extract_audio_features(content, file.filename)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # pragma: no cover - defensive API guard
        raise HTTPException(status_code=500, detail=f"Audio processing failed: {exc}") from exc

    inference = model_service.predict(features)
    report = build_diagnostic_report(file.filename, features, inference)

    return AnalysisResponse(
        file_name=file.filename,
        model_used=inference.model_used,
        predicted_condition=inference.predicted_condition,
        confidence=inference.confidence,
        severity=inference.severity,
        urgent_alert=inference.urgent_alert,
        explanation=inference.explanation,
        feature_summary=features.to_dict(),
        recommendations=inference.recommendations,
        report=report,
        differentials=inference.differentials,
    )
