# Sock

Android app for the "Sock on the Door" availability-sharing experience.

## Getting Started
- Install the latest Android Studio Iguana+ or ensure local JDK 17 is available.
- From the repo root:
  ```bash
  ./gradlew tasks
  ```
  The first run downloads the Android Gradle Plugin plus Compose/Room dependencies.

## Tech Stack
- Kotlin 1.9, Jetpack Compose (Material 3)
- Navigation Compose, Coroutines, Hilt
- Room (local persistence), DataStore (preferences)

## Project Layout
- `app/` ? Android application module with Compose UI and Hilt setup.
- `app/src/main/java/com/sock/app` ? App entry point, navigation host, screen implementations.
- `docs/` ? Requirements notes and ongoing product guidance.
- `sock_requirements.txt` ? Text extraction of the full Product Requirements Document (PRD).

## Next Steps
1. Flesh out the local-first data layer (Room repositories + DataStore preferences) to back the Compose UI.
2. Replace preview data in `SockApp` with repository-backed state and view models.
3. When preparing for a public launch, introduce cloud sync behind repository interfaces and guard it behind build flavors.