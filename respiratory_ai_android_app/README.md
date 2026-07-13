# Respiratory AI Android App

Native Android app workspace for the respiratory screening project.

## Proposed stack

- Kotlin
- Jetpack Compose
- Android SDK: `compileSdk 36`, `targetSdk 35`, `minSdk 26`
- Android Studio Panda 2 (`2025.3.2`)
- Android Gradle Plugin `9.1.0`
- JDK `17`
- Retrofit + OkHttp for API calls
- Kotlinx Serialization for JSON
- MVVM + Repository pattern
- Coroutines + Flow
- DataStore for small local settings
- Firebase Crashlytics and Analytics in release builds
- `AudioRecord` for live microphone capture
- WAV recording in app-private storage before upload

## Release target

- Google Play Android App Bundle (`.aab`)
- Android only, no iOS target

## Backend dependency

Version 1 will call the existing Python inference backend over HTTPS.

## Recording support

- Support two input modes: live microphone recording and existing file upload
- Request `android.permission.RECORD_AUDIO` at runtime
- Save recorded clips to app-private temporary storage and upload from there
- Use the Android system file picker for manual uploads
- Avoid broad media-library permissions unless we later add in-app audio browsing

## Current project structure

- `app/src/main/java/com/respiratoryai/mobile/MainActivity.kt`: app entry point
- `app/src/main/java/com/respiratoryai/mobile/ui/`: Compose screens and view-model state
- `app/src/main/java/com/respiratoryai/mobile/recording/`: microphone-to-WAV recording logic
- `app/src/main/java/com/respiratoryai/mobile/data/`: API models and repository
- `app/src/main/AndroidManifest.xml`: app permissions and Android registration
- `app/build.gradle.kts`: Android module config and dependencies

## Current status

- Android project scaffold created
- Compose UI created for record, upload, analyze, and results
- Mobile-style layout created with a structured home screen, status section, action section, result overview, probability breakdown, heatmap, and window insights
- Runtime microphone permission flow included
- Local WAV recording flow included
- Backend API contract wired to `/predict`
- Heatmap rendering support included for backend `data:image/png;base64,...` responses
- Gradle wrapper added locally with a bundled Gradle `9.3.1` distribution zip inside the project

## To run the UI locally

You can watch and edit all files in VS Code, but to actually run the Android UI you will usually want Android Studio plus an emulator.

Minimum local setup:

- Android Studio installed
- Android SDK installed for API 35 or 36
- The Python backend running and reachable from the emulator at `http://10.0.2.2:8080/`

## Open in Android Studio

1. Open Android Studio
2. Choose `Open`
3. Select `/Users/diyarao/Documents/Playground/respiratory_ai_android_app`
4. Let Gradle sync finish
5. Create or start an Android emulator
6. Run the `app` configuration

## Backend for emulator

The debug app is configured to call:

- `http://10.0.2.2:8080/`

That means the Flask backend should be running on your Mac at port `8080` while the emulator is running.

## Important note

In this terminal session, Gradle verification depends on whether the Android and Kotlin plugin artifacts are already cached locally or can be downloaded. Android Studio should still be the primary place to sync and run the UI.
