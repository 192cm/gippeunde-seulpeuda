<div align="center">

# 기쁜데 슬프다

**AI 감정 셀카 미션 SNS**
매일 주어지는 목표 감정 조합을 셀피 표정으로 재현하고, 머신러닝 감정 분석 결과를 점수화해 그룹 피드에 공유하는 Android 앱입니다.

<br>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-SDK%2035-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.00-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-2.17.0-FF6F00?style=flat-square&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)

</div>

---

## 프로젝트 개요

「기쁜데 슬프다」는 사용자가 매일 제시되는 감정 조합 미션에 맞춰 셀피를 촬영하고, 온디바이스 머신러닝 모델이 분석한 감정 비율을 바탕으로 점수를 산출하는 Android 감정 미션 앱입니다. 사용자는 자신이 참여한 그룹을 선택한 뒤, 그룹과 날짜를 기준으로 생성된 오늘의 목표 감정 조합을 확인하고 미션을 수행합니다.

앱의 기본 흐름은 그룹 선택, 오늘의 감정 미션 확인, 셀피 촬영 또는 갤러리 이미지 선택, 얼굴 검증, 감정 분석, 점수 확인, 위치 선택 및 결과 저장 순서로 구성되어 있습니다. 촬영된 이미지는 먼저 Google ML Kit Face Detection으로 얼굴이 정확히 1명인지 검사하고, 이후 TensorFlow Lite 감정 분석 모델로 `HAPPY`, `SAD`, `ANGRY`, `SURPRISED` 네 가지 감정 비율을 예측합니다.

최종 점수는 오늘의 목표 감정 비율과 실제 분석된 감정 비율의 차이를 비교해 0점부터 100점 사이로 계산됩니다. 결과 화면에서는 촬영 이미지, 감정별 분석 비율, 최종 점수, 피드백 문구, 위치 태그를 함께 확인할 수 있습니다.

---

## 팀원 소개

| 팀원 | 역할 분담 |
| --- | --- |
| 안준영 | 어플리케이션 전체 구조 기획, 화면 구성 및 와이어프레임 설계, 기본 득점 기능 구현, 머신러닝 모델 학습 데이터 정리 및 전처리, UI 동작 테스트 |
| 장건호 | 머신러닝 모델 제작 및 적용, 추가 득점 기능 구현, 득점 결과 처리 로직 구현, 어플리케이션 안정성 확보 기능 구현, PPT 자료 정리 및 발표 구성 |

안준영은 앱의 전체 구조와 사용 흐름을 기획하고, 주요 화면 구성을 바탕으로 와이어프레임을 설계했습니다. 또한 사용자가 입력하거나 선택한 값에 따라 점수가 계산되는 기본 득점 기능과 학습 데이터 정리, UI 동작 테스트를 담당했습니다.

장건호는 머신러닝 모델을 제작해 앱에 적용하고, 추가 득점 기능과 결과 처리 로직을 구현했습니다. 예외 상황에서도 앱 흐름이 끊기지 않도록 안정성 확보 기능을 보강했으며, 최종 발표 자료 정리와 발표 구성을 담당했습니다.

---

## 핵심 기능

| 기능 | 설명 |
| --- | --- |
| 일일 감정 미션 | 날짜와 그룹에 따라 서로 다른 목표 감정 조합을 생성합니다. |
| 얼굴 검증 | ML Kit Face Detection으로 사진 속 얼굴이 정확히 1명인지 확인합니다. |
| 감정 분석 | TensorFlow Lite 모델이 `HAPPY`, `SAD`, `ANGRY`, `SURPRISED` 감정 비율을 추론합니다. |
| 점수 계산 | 목표 감정 벡터와 실제 감정 벡터의 차이를 비교해 최종 점수를 산출합니다. |
| 그룹 피드 | 사용자가 그룹을 만들거나 초대 코드로 참여하고, 미션 결과를 공유합니다. |
| 랭킹 | 그룹 구성원의 미션 점수를 기준으로 순위를 제공합니다. |
| 아카이브 | Room DB에 저장된 이전 미션 기록을 슬라이드 형태로 다시 확인합니다. |
| 위치 태그 | GPS와 지도 기능을 활용해 결과에 위치 정보를 함께 저장합니다. |

---

## 화면 구성

필수 조건인 3개 이상의 Activity를 넘어 총 5개 Activity로 앱 흐름을 구성했습니다.

| Activity | 역할 | 핵심 UI |
| --- | --- | --- |
| `MainActivity` | 홈 / 미션 허브 | 오늘의 목표 감정, AI 한줄 문구, 그룹 선택, 네비게이션 드로어 |
| `MissionActivity` | 셀피 촬영 및 검증 | 카메라, 갤러리, 얼굴 1명 검증, TFLite 채점 |
| `ResultActivity` | 결과 확인 및 업로드 | 점수, 감정 벡터 비교, 지도, GPS 위치 태그 |
| `GroupActivity` | 그룹 피드 | 실시간 피드, 랭킹 탭, 초대 코드 |
| `ArchiveActivity` | 기록 아카이브 | 자동 재생 슬라이드, 전환 속도 조절 |

### 앱 흐름

```text
그룹 선택
  -> 오늘의 목표 감정 확인
  -> 셀피 촬영 또는 갤러리 이미지 선택
  -> 얼굴 1명 검증
  -> TFLite 감정 분석
  -> 목표 감정과 실제 감정 비교
  -> 점수 확인
  -> 위치 태그 선택
  -> Room 저장 및 그룹 피드 업로드
```

### Intent 데이터 전달

| # | 전달 방향 | 전달 데이터 |
| --- | --- | --- |
| 1 | `MainActivity` -> `MissionActivity` | `emotionTarget`, `groupId` |
| 2 | `MissionActivity` -> `ResultActivity` | `photo`, `emotionResult`, `score`, `emotionTarget`, `groupId` |
| 3 | `MainActivity` -> `GroupActivity` | `groupId` |
| 4 | `MainActivity` -> `ArchiveActivity` | `groupId`, `date` |
| 5 | `GroupActivity` -> `ArchiveActivity` | `groupId`, `date` |
| 6 | `ResultActivity` -> `MainActivity` | `FLAG_ACTIVITY_CLEAR_TOP` 복귀 |

감정 데이터는 Moshi로 JSON 직렬화한 뒤 Intent extra로 전달합니다.

---

## 구현 명세

| 항목 | 구현 내용 |
| --- | --- |
| Coroutine | `lifecycleScope`, `Dispatchers.IO`, `withContext`, Compose `LaunchedEffect`, Room `suspend` DAO, 자동 슬라이드쇼 코루틴 |
| 네트워크 | Retrofit, OkHttp, 로깅 인터셉터, Coil 이미지 로딩 |
| Jetpack Compose | `LazyColumn`, `LazyVerticalGrid`, `HorizontalPager`, `ModalNavigationDrawer`, `TabRow` |
| 외부 앱 연동 | 카메라 `TakePicture` + FileProvider, 갤러리 `GetContent` 인텐트 |
| 외부 API | Google Maps, Gemini, Nominatim/OSM 역지오코딩 |
| 머신러닝 | FER2013 기반 TensorFlow Lite 감정 분류 모델, ML Kit 얼굴 검출, 얼굴 영역 크롭 후 추론 |
| 로컬 DB | Room을 활용한 미션 기록 영구 저장 |
| 디바이스 연동 | FusedLocation GPS를 활용한 현재 위치 태그 |

---

## 안정성 설계

본 프로젝트는 발표와 실제 사용 상황에서 앱 흐름이 끊기지 않도록 외부 의존 지점마다 실패 폴백을 설계했습니다.

- 네트워크 API 호출은 `try-catch`로 감싸고, 실패 시 기본 문구나 핫스팟 이름으로 대체합니다.
- TFLite 모델 로드, 추론, 라벨 파싱 실패 시에도 앱이 종료되지 않도록 예외 처리를 적용합니다.
- 카메라와 갤러리 이미지 디코딩 실패 시 사용자에게 안내하고 이전 화면 흐름을 유지합니다.
- Kotlin nullable 타입, 세이프콜, 엘비스 연산자를 사용해 null 값을 안전하게 처리합니다.
- `lifecycleScope`와 `collectAsStateWithLifecycle`을 사용해 Activity 생명주기에 맞춰 비동기 작업을 관리합니다.
- 고해상도 이미지는 다운샘플링해 메모리 사용량을 줄이고, EXIF 회전을 보정해 실기기 사진이 올바르게 분석되도록 처리합니다.
- 카메라와 위치 권한이 거부되어도 프리셋 이미지나 기본 위치 값으로 대체할 수 있게 구성했습니다.
- 얼굴이 0명 또는 2명 이상 감지되면 업로드를 차단하고 명확한 안내를 표시합니다.

---

## 기술 스택

| 분류 | 기술 |
| --- | --- |
| 언어 | Kotlin |
| UI | Jetpack Compose, Material 3 |
| 비동기 | Kotlin Coroutine, Lifecycle Runtime |
| 로컬 저장소 | Room |
| 직렬화 | Moshi |
| 네트워크 | Retrofit, OkHttp |
| 이미지 | Coil |
| 머신러닝 | TensorFlow Lite, Google ML Kit Face Detection |
| 지도 / 위치 | Google Maps Compose, Google Play Services Location, Nominatim/OSM |
| 테스트 | JUnit, Robolectric, Roborazzi |
| 학습 환경 | Python, TensorFlow/Keras, FER2013 |

---

## 프로젝트 구조

```text
gippeunde-seulpeuda/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ assets/
│     │  ├─ emotion_labels.txt
│     │  └─ emotion_mobilenetv2.tflite
│     ├─ java/com/example/
│     │  ├─ MainActivity.kt
│     │  ├─ MissionActivity.kt
│     │  ├─ ResultActivity.kt
│     │  ├─ GroupActivity.kt
│     │  ├─ ArchiveActivity.kt
│     │  ├─ data/
│     │  ├─ ml/
│     │  └─ network/
│     └─ res/
├─ assets/
│  ├─ branding/
│  │  ├─ frontpage.png
│  │  └─ icon.png
│  └─ report/
│     ├─ p1-project-mark.png
│     └─ p3-wireframe-reference.jpg
├─ artifacts/
│  ├─ report/
│  │  ├─ report_capture.pdf
│  │  ├─ report_capture.pptx
│  │  ├─ report_capture.pptx.inspect.ndjson
│  │  └─ captures/
│  └─ wireframes/
│     └─ pnu_emotion_wireframe_board_with_real_photos.png
├─ docs/
│  └─ reports/
│     ├─ presentation.html
│     ├─ app_explanation.html
│     └─ architecture_explanation.html
├─ tools/
│  └─ make_wireframe_board_with_photos.py
├─ gradle/libs.versions.toml
├─ training/
│  ├─ environment.yml
│  ├─ train_mobilenetv2_fer2013.py
│  ├─ evaluate_tflite_fer2013.py
│  └─ README.md
└─ .env.example
```

---

## 실행 방법

### 1. 저장소 준비

```bash
git clone https://github.com/192cm/gippeunde-seulpeuda.git
cd gippeunde-seulpeuda
base64 -d debug.keystore.base64 > debug.keystore
```

Windows PowerShell에서는 다음 명령으로 debug keystore를 복원할 수 있습니다.

```powershell
[IO.File]::WriteAllBytes("debug.keystore", [Convert]::FromBase64String((Get-Content "debug.keystore.base64" -Raw)))
```

### 2. API 키 설정

```bash
cp .env.example .env
```

`.env`에 필요한 값을 설정합니다.

| Key | 설명 |
| --- | --- |
| `GEMINI_API_KEY` | 홈 화면의 일일 미션 문구 생성을 위한 Gemini API 키 |
| `MAPS_API_KEY` | 결과 화면의 Google Map 표시를 위한 Maps API 키 |

### 3. 빌드

```bash
./gradlew :app:assembleDebug
```

Windows PowerShell에서는 다음 명령을 사용할 수 있습니다.

```powershell
.\gradlew.bat :app:assembleDebug
```

### 4. 설치

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android Studio에서 실행하려면 프로젝트를 열고 Gradle Sync를 완료한 뒤 `app` 구성을 선택해 에뮬레이터 또는 실제 기기에서 실행합니다.

---

## 모델 학습

FER2013 데이터셋을 사용해 네 가지 감정 클래스에 대한 TensorFlow Lite 모델을 학습할 수 있습니다.

```bash
conda env create -f training/environment.yml
conda activate gippeunde-emotion
python training/train_mobilenetv2_fer2013.py --image-dir training/data/fer2013 --architecture fer-cnn
```

CSV 데이터셋을 사용하는 경우 다음처럼 실행할 수 있습니다.

```bash
python training/train_mobilenetv2_fer2013.py --csv training/data/fer2013.csv --architecture fer-cnn
```

학습 후 생성된 모델과 라벨 파일은 Android 앱에서 읽을 수 있도록 `app/src/main/assets`에 복사합니다.

```bash
cp training/exports/emotion_mobilenetv2.tflite app/src/main/assets/emotion_mobilenetv2.tflite
cp training/exports/emotion_labels.txt app/src/main/assets/emotion_labels.txt
```

---

## 차별점

이 앱은 단순히 사진을 저장하거나 공유하는 앱이 아니라, 사용자의 표정을 데이터로 분석하고 이를 목표 감정 조합과 비교해 점수화합니다. 또한 개인 결과 화면에서 끝나지 않고 그룹 피드, 랭킹, 아카이브로 확장해 미션 수행 결과를 소셜 경험으로 연결합니다.

특히 얼굴 검증, 온디바이스 감정 분석, 점수 계산, 위치 태그, 로컬 저장, 그룹 공유 기능을 하나의 미션 흐름으로 묶었다는 점에서 완성도 있는 Android 감정 미션 플랫폼을 목표로 합니다.

---

## 검증

발표 자료 기준 검증 결과:

```text
./gradlew :app:assembleDebug -> BUILD SUCCESSFUL
```

---

## 라이선스

이 저장소에는 별도의 라이선스 파일이 포함되어 있지 않습니다.
