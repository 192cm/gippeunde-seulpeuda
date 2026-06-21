<div align="center">

# 🙂 기쁜데 슬프다

**Android 감정 미션 앱** · 매일 정해진 그룹 감정 목표와 셀피 감정 분석 결과를 비교해 점수화합니다.

<br>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/) [![Android](https://img.shields.io/badge/Android-SDK%2035-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/) [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.00-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose) [![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-2.17.0-FF6F00?style=flat-square&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)

<br>

[기능](#-기능) · [빠른 시작](#-빠른-시작) · [사용법](#-사용법) · [설정](#-설정) · [아키텍처](#-아키텍처) · [AI 학습](#-ai-학습) · [의존성](#-의존성) · [라이선스](#-라이선스)

</div>

---

## ✨ 기능

- **일일 감정 미션** · 날짜와 그룹에 따라 `HAPPY`, `SAD`, `ANGRY`, `SURPRISED` 목표 비율을 생성합니다.
- **셀피 검증** · Google ML Kit Face Detection으로 사진 속 얼굴이 정확히 1명인지 확인합니다.
- **온디바이스 채점** · TensorFlow Lite 모델이 감정 벡터를 추론하고 목표 벡터와의 거리를 점수로 변환합니다.
- **그룹 피드** · 사용자는 그룹을 선택하거나 생성하고, 미션 결과를 업로드해 랭킹과 피드를 확인합니다.
- **로컬 기록 보관** · Room이 완료한 미션의 날짜, 사진 경로, 점수, 감정 결과, 좌표를 저장합니다.
- **학습 파이프라인** · FER2013 기반 학습 스크립트가 Android 앱에서 사용하는 `.tflite` 모델과 라벨 파일을 내보냅니다.

---

## 🚀 빠른 시작

### 1. 환경 준비

```bash
git clone https://github.com/192cm/gippeunde-seulpeuda.git
cd gippeunde-seulpeuda
base64 -d debug.keystore.base64 > debug.keystore
```

### 2. 인증 정보 / 설정

Gemini API 키는 Google AI Studio 무료 티어에서 만들 수 있습니다. 지도 기능을 실행하려면 Google Maps API 키도 `.env`에 넣습니다.

```bash
cp .env.example .env
```

### 3. 실행

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧭 사용법

### Android Studio

```bash
studio .
```

프로젝트를 열고 Gradle Sync를 완료한 뒤 에뮬레이터 또는 Android 기기를 선택해 `app` 구성을 실행합니다.

> Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용할 수 있습니다.

### 앱 흐름

| 화면 | 진입점 | 역할 |
|------|--------|------|
| `MainActivity` | 런처 | 그룹 선택, 오늘의 감정 목표 확인, 미션 시작 |
| `MissionActivity` | 미션 촬영 | 카메라 또는 갤러리 이미지 선택, 얼굴 수 검증, 감정 분석 |
| `ResultActivity` | 결과 확인 | 점수와 감정 비교 결과 표시, 위치 태그 선택, 기록 저장 |
| `GroupActivity` | 그룹 피드 | 그룹 제출 목록, 초대 코드, 랭킹 탭 표시 |
| `ArchiveActivity` | 아카이브 | 그룹과 날짜별 저장 미션 기록 표시 |

### 모델 학습

```bash
conda env create -f training/environment.yml
conda activate gippeunde-emotion
python training/train_mobilenetv2_fer2013.py --image-dir training/data/fer2013 --architecture fer-cnn
```

> 앱은 `app/src/main/assets`의 모델 파일을 읽으므로 새 모델을 학습한 뒤 export 결과를 해당 경로로 복사합니다.

---

## ⚙️ 설정

모든 런타임 키는 `.env`에서 관리합니다 · Gemini 문구 생성과 Google Maps 표시를 코드 수정 없이 설정할 수 있습니다.

| Key | Default | Description |
|-----|---------|-------------|
| `GEMINI_API_KEY` | `MY_GEMINI_API_KEY` | 홈 화면의 일일 미션 문구 생성을 위한 Gemini API 키 |
| `MAPS_API_KEY` | `MY_MAPS_API_KEY` | 결과 화면의 Google Map 표시를 위한 Maps API 키 |

추가 빌드와 모델 설정은 프로젝트 파일에서 관리합니다.

| 파일 | 설명 |
|------|------|
| `gradle/libs.versions.toml` | Android, Kotlin, Compose, Room, ML Kit, TensorFlow Lite 버전 관리 |
| `app/build.gradle.kts` | SDK 레벨, 서명 설정, Compose, Secrets Gradle Plugin, 앱 의존성 정의 |
| `training/environment.yml` | FER2013 모델 학습용 Conda 환경 정의 |
| `app/src/main/assets/emotion_labels.txt` | 앱 추론 결과가 따르는 감정 라벨 계약 |

---

## 🏗️ 아키텍처

```
gippeunde-seulpeuda/
├─ app/
│  ├─ build.gradle.kts              # Android 앱 빌드 설정
│  └─ src/main/
│     ├─ assets/
│     │  ├─ emotion_labels.txt      # 감정 라벨 계약
│     │  └─ emotion_mobilenetv2.tflite
│     ├─ java/com/example/
│     │  ├─ MainActivity.kt         # 홈과 미션 진입
│     │  ├─ MissionActivity.kt      # 촬영과 분석 흐름
│     │  ├─ ResultActivity.kt       # 점수 확인과 저장
│     │  ├─ GroupActivity.kt        # 그룹 피드와 랭킹
│     │  ├─ ArchiveActivity.kt      # 저장 기록 화면
│     │  ├─ data/                   # Room 저장소와 mock feed
│     │  ├─ ml/                     # 얼굴 검출과 감정 분석
│     │  └─ network/                # Gemini, 지도, 역지오코딩 API
│     └─ res/                       # Android 리소스
├─ gradle/libs.versions.toml        # 의존성 버전 카탈로그
├─ training/
│  ├─ environment.yml               # 학습 환경
│  ├─ train_mobilenetv2_fer2013.py  # FER2013 학습 스크립트
│  ├─ exports/                      # TFLite와 라벨 export
│  └─ runs/                         # Keras 체크포인트
└─ .env.example                     # API 키 템플릿
```

```
그룹별 일일 목표
   │ 목표 감정 JSON
   ▼
MainActivity.kt ─────────▶ MissionActivity.kt
                              │ 사진 Bitmap
                              ▼
                     FaceAndEmotionAnalyzer.kt ─────▶ ML Kit Face Detection
                              │ 얼굴 1명 검증
                              ▼
                     TensorFlow Lite model
                              │ 4개 감정 벡터
                              ▼
ResultActivity.kt ─────────▶ Room database
        │                     │ 저장된 미션 기록
        │                     ▼
        └────────────▶ FirebaseRemoteMock.kt ─────▶ GroupActivity.kt
                       mock feed item              그룹 피드와 랭킹
```

> 앱은 얼굴 검출과 감정 추론을 기기 안에서 처리하고, 소셜 피드는 로컬 mock 저장소로 유지해 Firebase 없이도 전체 미션 흐름을 테스트할 수 있습니다.

---

## 🧠 AI 학습

학습 파이프라인은 FER2013 데이터를 네 가지 감정 클래스의 Android 분류기로 변환합니다.

| 입력 | 경로 | 설명 |
|------|------|------|
| 폴더 데이터셋 | `training/data/fer2013` | `train/`과 `test/` 또는 `validation/` 클래스 폴더 사용 |
| CSV 데이터셋 | `training/data/fer2013.csv` | `emotion`, `pixels`, `Usage` 컬럼 사용 |
| 모델 export | `training/exports/emotion_mobilenetv2.tflite` | Android assets로 복사할 TFLite 모델 |
| 라벨 export | `training/exports/emotion_labels.txt` | Android assets로 복사할 라벨 목록 |

```bash
python training/train_mobilenetv2_fer2013.py --csv training/data/fer2013.csv --architecture fer-cnn
cp training/exports/emotion_mobilenetv2.tflite app/src/main/assets/emotion_mobilenetv2.tflite
cp training/exports/emotion_labels.txt app/src/main/assets/emotion_labels.txt
```

---

## 📦 의존성

| 의존성 | 역할 |
|--------|------|
| Jetpack Compose Material 3 | 앱 UI |
| Google ML Kit Face Detection | 얼굴 수 검증 |
| TensorFlow Lite | 온디바이스 감정 추론 |
| Room | 로컬 미션 기록 저장 |
| Moshi | 감정 벡터와 피드 JSON 직렬화 |
| Google Maps Compose | 결과 화면 지도 표시 |
| Google Play Services Location | 현재 위치 태그 |
| Retrofit, OkHttp | Gemini와 역지오코딩 API 호출 |
| Robolectric, Roborazzi | JVM 테스트와 스크린샷 테스트 |

---

## 📄 라이선스

이 저장소에는 별도의 라이선스 파일이 포함되어 있지 않습니다.
