# Respiratory Disease Screening MVP

This repository turns your abstract into a practical starter project: a web app that accepts breathing-sound recordings, extracts acoustic features, runs a swappable screening model, and returns a condition label, severity estimate, urgent alert, explainability regions, and a short diagnostic report.

## What is included

- `FastAPI` backend with an `/api/analyze` endpoint for uploading respiratory audio.
- Audio feature extraction using `librosa` when available, with a graceful fallback for minimal environments.
- A baseline inference service that keeps the end-to-end workflow working until you attach trained CNN, ResNet-50, VGG16, Inception, or CNN-LSTM weights.
- A polished single-page frontend for uploads, results, explanations, and report display.
- A small test that sanity-checks report generation.

## Project structure

```text
app/
  main.py
  schemas.py
  services/
    audio.py
    inference.py
    reporting.py
static/
  index.html
  styles.css
  app.js
tests/
  test_reporting.py
requirements.txt
```

## Quick start

1. Create a virtual environment:

   ```bash
   python3 -m venv .venv
   source .venv/bin/activate
   ```

2. Install dependencies:

   ```bash
   pip install -r requirements.txt
   ```

3. Run the app:

   ```bash
   uvicorn app.main:app --reload
   ```

4. Open `http://127.0.0.1:8000`.

## Current architecture

### 1. Input layer

The UI accepts audio captured from:

- Digital stethoscopes
- Smartphones
- Raspberry Pi or other low-cost edge devices

### 2. Feature extraction

The MVP extracts or prepares:

- MFCCs
- Chromagram summaries
- Spectral centroid
- Zero-crossing rate
- RMS energy

These give you a foundation for spectrogram-based training and future model experimentation.

### 3. Inference layer

Right now the project uses a baseline heuristic service so the complete product flow works immediately.

To replace it with a trained model later:

1. Train your CNN / ResNet-50 / VGG16 / Inception / CNN-LSTM model on respiratory datasets.
2. Save weights and preprocessing metadata.
3. Update `app/services/inference.py` so `RespiratoryModelService.predict()` loads your real model and returns the same response shape.

## Recommended next build steps

### Train the real classifier

- Convert audio into mel spectrograms or stacked feature tensors.
- Train with datasets such as ICBHI or similar respiratory-sound collections.
- Add evaluation metrics: accuracy, F1-score, sensitivity, specificity, ROC-AUC.

### Improve explainability

- Add Grad-CAM or attention heatmaps over spectrogram windows.
- Surface highlighted time regions in the UI instead of placeholder explanation segments.

### Support deployment targets

- Containerize the API for cloud deployment.
- Add offline-friendly batching for Raspberry Pi.
- Export a lighter inference model for edge devices.

### Expand clinical output

- Add PDF report export.
- Track patient metadata and visit history.
- Add threshold-based emergency escalation workflows.

## Important note

The current classifier is an MVP placeholder, not a medical device or clinically validated diagnostic model. Use it as a project foundation and replace the inference layer with a trained and validated model before any real-world clinical use.
