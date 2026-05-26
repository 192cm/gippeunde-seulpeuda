# MobileNetV2 FER2013 Fine-Tuning

This folder contains the local training pipeline for producing the emotion
classifier that will later be copied into the Android app as a `.tflite` asset.

## Target App Contract

The Android app currently expects these emotion labels:

```text
HAPPY
SAD
ANGRY
SURPRISED
NEUTRAL
FEAR
DISGUST
```

The model is trained as a seven-class FER2013 classifier, including `DISGUST`.

## Expected Inputs

Use the folder format by default. Most FER2013 downloads look like this:

```text
training/data/fer2013/
  train/
    angry/
    disgust/
    fear/
    happy/
    neutral/
    sad/
    surprise/
  test/
    angry/
    disgust/
    fear/
    happy/
    neutral/
    sad/
    surprise/
```

The script accepts `test/` or `validation/` for validation images.

CSV is also supported if you specifically have the older Kaggle CSV:

```text
training/data/fer2013.csv
```

The CSV must contain:

```text
emotion,pixels,Usage
```

## Train

From the repo root:

```powershell
conda env create -f training\environment.yml
conda activate gippeunde-emotion
python training\train_mobilenetv2_fer2013.py --image-dir training\data\fer2013
```

Or with the CSV format:

```powershell
python training\train_mobilenetv2_fer2013.py --csv training\data\fer2013.csv
```

## Outputs

The script writes:

```text
training/exports/emotion_mobilenetv2.tflite
training/exports/emotion_labels.txt
training/runs/best_emotion_mobilenetv2.keras
```

After training, copy the exported files to:

```text
app/src/main/assets/emotion_mobilenetv2.tflite
app/src/main/assets/emotion_labels.txt
```

Android inference is already wired to load these assets through
`FaceAndEmotionAnalyzer`.
