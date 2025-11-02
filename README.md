# Sock

Android app for the "Sock on the Door" availability-sharing experience.

## Getting Started
- Install the latest Android Studio Iguana+ or ensure local JDK 17 is available.
- Copy your Firebase `google-services.json` into `app/src/debug` and `app/src/release` before enabling the Google Services plugin.
- From the repo root:
  ```bash
  ./gradlew tasks
  ```
  The first run downloads the Android Gradle Plugin, Compose, and Firebase libraries.

## Tech Stack
- Kotlin 1.9, Jetpack Compose (Material 3)
- Navigation Compose, Coroutines, Hilt
- Firebase Auth, Firestore, Storage, Functions (via BOM)

## Project Layout
- `app/` ? Android application module with Compose UI and Hilt setup.
- `app/src/main/java/com/sock/app` ? App entry point, navigation host, Screen implementations.
- `docs/` ? Requirements notes and ongoing product guidance.
- `sock_requirements.txt` ? Text extraction of the full Product Requirements Document (PRD).

## Next Steps
1. Wire Firebase dependencies and add `google-services` plugin once configs are checked in.
2. Replace preview data in `SockApp` with repository-backed state (Firestore realtime listeners).
3. Implement authentication flows aligned with the MVP requirements in `Sock on the Door App.pdf`.