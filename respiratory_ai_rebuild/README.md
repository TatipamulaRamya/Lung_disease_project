# Respiratory AI Rebuild

Clean rebuild of the respiratory sound classification project with:

- leakage-safe dataset preparation and splitting
- one shared preprocessing pipeline for training and inference
- configurable TensorFlow models for spectrogram classification
- honest validation/test evaluation
- real backend inference instead of filename-based mock responses

## Project layout

- `configs/`: training and inference configuration
- `data/`: generated metadata, splits, and optional caches
- `models/`: saved model artifacts and reports
- `src/resp_ai/`: reusable training and inference code
- `app/backend/`: Flask inference API
- `app/frontend/`: minimal browser client

## Suggested workflow

1. Install dependencies:

```bash
pip install -r requirements.txt
```

2. Prepare the merged dataset from the original project folder:

```bash
python -m resp_ai.data.prepare_dataset \
  --major-project-root "/Users/diyarao/Major Project" \
  --output-root "/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/data"
```

3. Create leakage-safe splits:

```bash
python -m resp_ai.data.create_splits \
  --metadata "/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/data/metadata/master_metadata.csv" \
  --output-root "/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/data"
```

4. Train a model:

```bash
PYTHONPATH=src python -m resp_ai.models.train \
  --config configs/train.yaml
```

5. Evaluate the saved model on the untouched test split:

```bash
PYTHONPATH=src python -m resp_ai.models.evaluate \
  --config configs/train.yaml \
  --model-path models/latest/best_model.keras
```

6. Start the backend:

```bash
PYTHONPATH=src python app/backend/main.py
```

7. Open `app/frontend/index.html` in a browser and upload an audio file.

## Public deployment

The rebuild backend can be hosted publicly as a Docker web service. The project now includes:

- [Dockerfile](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/Dockerfile)
- [.dockerignore](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/.dockerignore)
- [DEPLOY_PUBLIC_BACKEND.md](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/DEPLOY_PUBLIC_BACKEND.md)

This deployment path is designed to ship only the Flask backend plus the production model files, not the multi-gigabyte training datasets.

## Expanding The Dataset

If you collect more matched-source recordings, use the manifest-driven prep path instead of editing Python scripts by hand:

```bash
PYTHONPATH=src python -m resp_ai.data.prepare_extended_dataset \
  --manifest configs/data_sources_extended.example.yaml
```

The example manifest is at [data_sources_extended.example.yaml](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/configs/data_sources_extended.example.yaml). It already includes the current ICBHI and chest-wall sources, and you can append new `manifest_csv` entries for any extra datasets you obtain.

For new sources, use the CSV template at [external_source_metadata_template.csv](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/data/external/templates/external_source_metadata_template.csv). Each row should describe one audio file with at least:

- `file_path`
- `patient_id`
- `raw_label`

Once you rebuild the merged dataset, run the strict curation and split steps again on the new metadata.

To see which source/class pairs are still underrepresented, generate a gap report like this:

```bash
PYTHONPATH=src python -m resp_ai.data.report_source_gaps \
  --metadata data_paper/metadata/curated_metadata.csv \
  --output reports/source_gap_targets_40_core.json \
  --include-sources icbhi,chest_wall \
  --target-per-source-class 40
```

The current matched-source report is saved at [source_gap_targets_40_core.json](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/reports/source_gap_targets_40_core.json).

If you want a training-ready augmented variant that lifts weak source/class pairs and weak classes using synthetic train clips only, build it like this:

```bash
PYTHONPATH=src python -m resp_ai.data.build_gap_augmented_dataset \
  --config configs/train_strong_cnn_gap_augmented.yaml \
  --base-data-root dataset_final \
  --output-root dataset_final_gap_augmented \
  --min-source-class-clips 120 \
  --min-class-clips 500
```

The training config for that dataset is [train_strong_cnn_gap_augmented.yaml](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/configs/train_strong_cnn_gap_augmented.yaml).

## Local VS Code workflow

For local development in VS Code, open the project folder and use the bundled workspace settings in [.vscode/settings.json](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/.vscode/settings.json).

Recommended local tasks:

- `RespAI: Smoke Test Pipeline` verifies that dataset paths, preprocessing, and model forward pass all work.
- `RespAI: Train Final Model (Local)` runs a lighter local config from [train_final_local.yaml](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/configs/train_final_local.yaml).
- `RespAI: Evaluate Latest Local Model` evaluates the latest local artifact on the test split.
- `RespAI: Run Backend` starts the Flask API for local inference testing.

The local machine can run the full pipeline, but full training is CPU-only and will be much slower than Colab GPU. For best-accuracy training runs, use Colab. For debugging, smoke tests, backend work, and smaller local experiments, the VS Code workflow is a good fit.

## Using The Best Colab Model Locally

The backend is configured to use the current best final model family from [train_strong_cnn_pneumonia_focus.yaml](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/configs/train_strong_cnn_pneumonia_focus.yaml). To run that model locally, copy the Colab-exported files into:

```text
/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/models_strong_cnn_pneumonia_focus/latest/
```

At minimum, this folder should contain:

- `best_model.keras`
- `class_names.json`

If you prefer another exported checkpoint location, set:

```bash
RESP_AI_MODEL=/absolute/path/to/best_model.keras
```

The backend health endpoint now reports both the active model path and whether the file exists, which makes it easier to verify the local deployment before uploading audio.

## Notes

- Group-aware splitting is used to keep recordings from the same patient in one split whenever a patient id can be inferred.
- Augmentation is applied only in the training dataset pipeline.
- The backend and training code use the exact same preprocessing settings from config.
- The default backend config is [train_strong_cnn_pneumonia_focus.yaml](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/configs/train_strong_cnn_pneumonia_focus.yaml), which uses the best current pneumonia-focused strong-CNN model family.
- Final inference uses recording-level aggregation across multiple overlapping 5-second windows instead of relying on a single clip.
