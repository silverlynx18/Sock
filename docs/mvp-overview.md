# Sock MVP Overview

## Product Pillars
- Real-time availability sharing inside invite-only groups.
- Respectful privacy defaults with explicit user control.
- Simple, Material Design 3-first experience tuned for Android.

## Core User Flows (MVP)
- **Authentication**: Email + password signup/login with display name, username, phone number capture and ToS/Privacy confirmation.
- **Profile management**: View profile, edit display name, upload avatar, view shared groups, initiate logout, stage account deletion.
- **Status system**:
  - Seven predefined global statuses with iconography.
  - Group-specific overrides with revert-to-global control.
  - Last-write-wins logic with real-time updates.
- **Groups**:
  - Create groups with name, color theme, avatar.
  - Role model (owner/admin/member) with owner transfer automation.
  - Manage members, leave/delete group.
  - Group page showing member roster + statuses.
- **Invitations**:
  - Shareable invite links processed via Cloud Functions.
  - Acceptance flow with confirmations and pending invite management.
- **Navigation**:
  - Dashboard with global card + group list.
  - Right-hand navigation rail toggled by couch button.
  - Dedicated Manage Groups, Settings, Profile surfaces.

## Technical Foundations
- Kotlin + Jetpack Compose, Material 3, Navigation Compose.
- Firebase Auth, Firestore, Storage, Functions with offline persistence.
- Hilt DI, Coroutines, Firebase BOM for dependency management.
- Gradle 8.7 wrapper, AGP 8.5.2, Kotlin 1.9.24.

## Near-Term Implementation Sequence
1. Lay down project scaffolding (done).
2. Wire Compose UI skeleton screens against fake data (done).
3. Integrate Hilt modules + repository contracts for Firebase.
4. Implement Firebase Auth onboarding with validation + Terms acceptance.
5. Define Firestore data models + Security Rules baseline.
6. Build group/status workflows with real-time listeners and offline caching.
7. Harden invitations flow with Cloud Functions + deep link handling.
8. Finish Settings (account deletion) and polish nav interactions.

## Risks & Notes
- `google-services.json` required before enabling Firebase plugins.
- Status consistency and owner transfer require Cloud Functions orchestration.
- Compose previews rely on stub data; ensure removal or gating before prod builds.
