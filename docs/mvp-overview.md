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
  - Role model (owner/admin/member) with owner transfer automation (simulated locally).
  - Manage members, leave/delete group.
  - Group page showing member roster + statuses.
- **Invitations**:
  - Shareable invite links simulated locally via generated codes; remote processing reserved for production launch.
  - Acceptance flow with confirmations and pending invite management.
- **Navigation**:
  - Dashboard with global card + group list.
  - Right-hand navigation rail toggled by couch button.
  - Dedicated Manage Groups, Settings, Profile surfaces.

## Technical Foundations
- Kotlin + Jetpack Compose, Material 3, Navigation Compose.
- Hilt DI, Coroutines, Room (local persistence), DataStore (preferences/config), optional KSP for schema generation.
- Repository interfaces abstract future remote sync so cloud hosting can be introduced only for public deployments.
- Gradle 8.7 wrapper, AGP 8.5.2, Kotlin 1.9.24.
- SwiftUI (iOS) + Swift Package Manager for parallel native development; React + Vite (Web) sharing design tokens and data contracts.

## Near-Term Implementation Sequence
1. Lay down project scaffolding (done).
2. Wire Compose UI skeleton screens against fake data (done).
3. Introduce Room entities/DAOs + repositories aligned with future sync requirements.
4. Implement authentication flow backed by local encrypted storage (pluggable for remote later).
5. Model status/group/invitation data schemas with local-first business rules.
6. Add background sync hooks that can no-op locally but integrate with cloud services when enabled.
7. Harden invitations flow with shareable codes and optional deep link handling.
8. Finish Settings (account deletion) and polish nav interactions.

## Post-MVP Enhancements (Local-First Roadmap)
- **Custom Status Studio**: Allow users to craft reusable statuses with tone presets and contextual notes (see `CustomStatus` model).
- **Shared Moments Board**: Lightweight, opt-in bulletin for highlights or planned hangouts stored locally with optional export/share.
- **Advanced Notification Controls**: Granular quiet hours and per-group overrides powered by DataStore.
- **Multi-device Sync Ready**: Repository interfaces expose change feeds to support future P2P or server sync without altering UI layers.
- **Accessibility & Personalization**: High-contrast palette options, typography scaling presets, and motion reduction toggles.

## Cross-Platform Alignment
- Maintain a single set of design tokens (color, typography, spacing) consumed by Android Compose, SwiftUI, and Tailwind.
- Track feature readiness in a shared board: API ready ? Android shipped ? iOS shipped ? Web shipped.
- Keep preview/local data shape identical across platforms (`SockUiState`, `SockAppState`, `SockState`) to ease shared repository work.
- Use automated lint/test pipelines per platform (Gradle, swift test, npm run lint/build) and schedule weekly triage for cross-platform issues.

## Risks & Notes
- Avoid cloud plugins until a public rollout; keep remote dependencies behind feature flags/flavors.
- Status consistency and owner transfer automation must work offline; upgrade to server-driven workflows only when cloud sync is available.
- Compose previews rely on stub data; ensure removal or gating before prod builds.
