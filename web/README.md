# Sock Web

React + Vite scaffold for the Sock experience. Mirrors the Android Compose and SwiftUI layouts with local preview data.

## Commands
- `npm install` ? install dependencies.
- `npm run dev` ? start the Vite dev server.
- `npm run build` ? type-check and build for production.
- `npm run lint` ? run ESLint with TypeScript rules.

## Structure
- `src/App.tsx` ? top-level router + layout.
- `src/components/` ? Dashboard and navigation rail components.
- `src/state/previewState.ts` ? shared mock data matching other platforms.
- `src/utils/date.ts` ? utility helpers (local-first, no remote deps).

Add real repositories/state management before connecting to a backend. Keep the local-first approach until a public cloud deployment is required.
