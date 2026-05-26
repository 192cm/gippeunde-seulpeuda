<div align="center">

# 🎭 Gippeunde Seulpeuda

**Android emotion mission app** — It scores selfie emotions against daily group targets.

<br>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/) [![Android](https://img.shields.io/badge/Android-SDK%2036-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/) [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.00-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose) [![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-2.17.0-FF6F00?style=flat-square&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)

<br>

[Features](#features) · [Quick Start](#quick-start) · [Usage](#usage) · [Configuration](#configuration) · [Architecture](#architecture) · [AI Training](#ai-training) · [Dependencies](#dependencies) · [License](#license)

</div>

---

## ✨ Features

- **Daily emotion missions** — The app generates group-specific target emotion mixes for each date.
- **Selfie validation** — ML Kit checks that a submitted photo contains exactly one face.
- **On-device scoring** — TensorFlow Lite predicts seven emotion classes and compares them with the target vector.
- **Group feed** — Users join or create mock groups, publish mission results, and view rankings.
- **Local history** — Room stores completed missions with photo path, score, emotions, and coordinates.
- **Training pipeline** — A FER2013 MobileNetV2 workflow exports the `.tflite` model and label file used by Android.

---

## 🚀 Quick Start

### 1. Environment setup

```bash
git clone https://github.com/192cm/gippeunde-seulpeuda.git
cd gippeunde-seulpeuda
base64 -d debug.keystore.base64 > debug.keystore
```

### 2. Credentials / config

Create `.env` from the example file. Gemini API keys can be created from the free tier in Google AI Studio.

```bash
cp .env.example .env
```

### 3. Run

```bash
gradle :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📖 Usage

### Android Studio

```bash
studio .
```

Open the project, let Gradle sync, select an emulator or Android device, then run the `app` configuration.

> The repository does not include a Gradle wrapper, so Android Studio or a local `gradle` installation must provide the build runtime.

### App Flow

| Screen | Route | Purpose |
|--------|-------|---------|
| `MainActivity` | Launcher | Selects a group, shows today's target, and opens mission tools |
| `MissionActivity` | Mission capture | Captures or selects a photo, validates face count, and runs emotion analysis |
| `ResultActivity` | Score review | Shows the score, emotion comparison, and campus location selector |
| `GroupActivity` | Group feed | Shows group submissions, invite-code flows, and ranking tabs |
| `ArchiveActivity` | Archive | Displays saved mission history for a group/date |

### Training

```bash
conda env create -f training/environment.yml
conda activate gippeunde-emotion
python training/train_mobilenetv2_fer2013.py --image-dir training/data/fer2013
```

> The app loads model assets from `app/src/main/assets`, so copy new exports there after training.

---

## ⚙️ Configuration

Everything is controlled through `.env` — no code changes needed to provide the Gemini key used by AI Studio scaffolding.

| Key | Default | Description |
|-----|---------|-------------|
| `GEMINI_API_KEY` | `MY_GEMINI_API_KEY` | API key read by the Secrets Gradle Plugin from `.env` |

Additional build and model settings live in project files:

| File | Description |
|------|-------------|
| `gradle/libs.versions.toml` | Centralizes Android, Kotlin, Compose, Room, ML Kit, and TensorFlow Lite versions |
| `app/build.gradle.kts` | Defines SDK levels, signing configs, Compose support, assets handling, and app dependencies |
| `training/environment.yml` | Defines the Conda environment for FER2013 model training |
| `app/src/main/assets/emotion_labels.txt` | Defines the seven emotion labels expected by inference |

---

## 🏗️ Architecture

```
gippeunde-seulpeuda/
├── app/
│   ├── build.gradle.kts              # Android app build config
│   └── src/main/
│       ├── assets/
│       │   ├── emotion_labels.txt    # Emotion class contract
│       │   └── emotion_mobilenetv2.tflite
│       ├── java/com/example/
│       │   ├── MainActivity.kt       # Home and mission entry
│       │   ├── MissionActivity.kt    # Capture and analysis flow
│       │   ├── ResultActivity.kt     # Score and save flow
│       │   ├── GroupActivity.kt      # Group feed and rankings
│       │   ├── ArchiveActivity.kt    # Saved mission archive
│       │   ├── data/                 # Room and mock feed storage
│       │   └── ml/                   # Face and emotion analyzer
│       └── res/                      # Android resources
├── gradle/libs.versions.toml         # Dependency versions
├── training/
│   ├── environment.yml               # Training environment
│   ├── train_mobilenetv2_fer2013.py  # FER2013 trainer
│   ├── exports/                      # TFLite and label outputs
│   └── runs/                         # Keras checkpoints
└── .env.example                      # Secrets template
```

```
Daily group target
   │  target emotion JSON
   ▼
MainActivity.kt ──▶ MissionActivity.kt
                         │  photo bitmap
                         ▼
              FaceAndEmotionAnalyzer.kt ──▶ ML Kit Face Detection
                         │  one-face validation
                         ▼
                    TensorFlow Lite model
                         │  seven-class emotion vector
                         ▼
ResultActivity.kt ──▶ Room database
        │                 │  saved mission record
        │                 ▼
        └──────────▶ FirebaseRemoteMock.kt ──▶ GroupActivity.kt
                       mock feed item          group feed and rankings
```

> The app keeps inference on device and treats remote social features as a local mock so mission flow stays testable without Firebase setup.

---

## 🤖 AI Training

The training workflow builds the Android emotion classifier from FER2013 in either folder or CSV format.

| Input | Path | Description |
|-------|------|-------------|
| Folder dataset | `training/data/fer2013` | Expects `train/` and `test/` or `validation/` class folders |
| CSV dataset | `training/data/fer2013.csv` | Expects `emotion`, `pixels`, and `Usage` columns |
| Model export | `training/exports/emotion_mobilenetv2.tflite` | TFLite model copied into Android assets |
| Label export | `training/exports/emotion_labels.txt` | Label list copied into Android assets |

```bash
python training/train_mobilenetv2_fer2013.py --csv training/data/fer2013.csv
cp training/exports/emotion_mobilenetv2.tflite app/src/main/assets/emotion_mobilenetv2.tflite
cp training/exports/emotion_labels.txt app/src/main/assets/emotion_labels.txt
```

---

## 📦 Dependencies

| Dependency | Role |
|------------|------|
| Jetpack Compose Material 3 | App UI |
| AndroidX Camera | Camera capture support |
| Google ML Kit Face Detection | Face count validation |
| TensorFlow Lite | On-device emotion inference |
| Room | Local mission persistence |
| Moshi | Emotion and feed JSON serialization |
| Google Play Services Location | Campus coordinate tagging |
| Robolectric and Roborazzi | JVM and visual test tooling |

---

## 📄 License

No license file is included in this repository.
