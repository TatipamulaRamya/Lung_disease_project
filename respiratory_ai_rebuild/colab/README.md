# Colab GPU Setup

Use [train_final_colab.ipynb](/Users/diyarao/Documents/Playground/respiratory_ai_rebuild/colab/train_final_colab.ipynb) in Google Colab.

What to place in Google Drive:

- `respiratory_ai_rebuild_colab_project.zip`
- `dataset_final.zip`

Recommended Drive layout:

- `MyDrive/respiratory_ai_rebuild_colab_project.zip`
- `MyDrive/dataset_final.zip`

The notebook will:

- mount Google Drive
- unzip the project into `/content/respiratory_ai_rebuild`
- extract `dataset_final.zip` from Drive into `/content/dataset_final`
- symlink `dataset_final` into the project root
- install Python dependencies
- train the model with `configs/train_final.yaml`
- evaluate the best model on the test split
- copy `models_final/` back into Drive

Why this layout:

- the code bundle stays small
- the dataset can live in Drive as a single upload
- the config paths remain unchanged
