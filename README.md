# Sock App

A social availability app for Android that helps friends and groups coordinate in-person hangouts by sharing real-time availability status.

## Overview

"Sock" is inspired by the simple concept of a "sock on the door" - a universally understood signal of availability. The app allows users to:
- Set global or group-specific status to indicate availability
- Create and manage groups of friends
- View who's available within their groups
- Join groups via invite links

## Tech Stack

### Android App
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **HTTP Client**: Ktor
- **JSON Serialization**: Kotlinx Serialization
- **Design**: Material Design 3

### Backend
- **Runtime**: Node.js
- **Framework**: Express.js
- **Database**: SQLite (suitable for <100 users)
- **Authentication**: JWT tokens
- **Password Hashing**: bcryptjs

## Project Structure

```
.
??? app/                          # Android app
?   ??? src/main/java/com/sock/app/
?       ??? data/
?       ?   ??? api/              # REST API client
?       ?   ??? model/            # Data models
?       ?   ??? repository/       # Data access layer
?       ??? ui/
?       ?   ??? navigation/       # Navigation setup
?       ?   ??? screens/          # UI screens
?       ?   ??? theme/            # App theme
?       ??? MainActivity.kt
??? backend/                      # Node.js/Express API
    ??? routes/                   # API route handlers
    ??? middleware/               # Auth middleware
    ??? scripts/                  # Database initialization
    ??? server.js                 # Entry point
```

## Current Status

### ? Completed
- [x] Project structure and Gradle configuration
- [x] Core data models (User, Group, Invitation, Status)
- [x] REST API client setup (Ktor)
- [x] Repository layer refactored for REST API
- [x] Backend API server (Node.js/Express)
- [x] SQLite database schema
- [x] Authentication endpoints (signup/signin)
- [x] User management endpoints
- [x] Group management endpoints
- [x] Invitation system endpoints
- [x] Basic navigation setup
- [x] Authentication screens (Login, Sign Up) - UI only
- [x] Dashboard screen placeholder

### ?? In Progress
- [ ] Complete ViewModels with API integration
- [ ] Dashboard with status system
- [ ] Group management screens
- [ ] Real-time updates (polling or WebSocket)

### ?? TODO
- [ ] Implement ViewModels for all screens
- [ ] Complete Dashboard with Global Status and Group Elements
- [ ] Create Group Page and Group Details screens
- [ ] Implement Status selection UI
- [ ] Create Manage Groups screen
- [ ] Implement invitation acceptance flow
- [ ] Add right-hand navigation bar
- [ ] Add profile picture upload functionality
- [ ] Implement user profile screens
- [ ] Add WebSocket support for real-time updates (optional)

## Setup Instructions

### Prerequisites
- **Android Development**: Android Studio Hedgehog (2023.1.1) or later, JDK 17+
- **Backend**: Node.js 18+ and npm

### Backend Setup

1. **Navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and set a secure JWT_SECRET
   ```

4. **Start the server**
   ```bash
   npm start
   # Or for development with auto-reload:
   npm run dev
   ```

   The API will be available at `http://localhost:3000/api`

### Android App Setup

1. **Open in Android Studio**
   - Open the project in Android Studio
   - Sync Gradle files
   - Let Android Studio download dependencies

2. **Configure API endpoint**
   - Update `ApiConfig.BASE_URL` in `app/src/main/java/com/sock/app/data/api/ApiService.kt`
   - For Android emulator: use `http://10.0.2.2:3000/api` (emulator's localhost)
   - For physical device: use your computer's IP address, e.g., `http://192.168.1.XXX:3000/api`

3. **Build and run**
   - Connect an Android device or start an emulator
   - Click Run in Android Studio

## Key Features (MVP)

### Status System
- 7 predefined status options:
  - Open to Hangout
  - Busy
  - Going Through it
  - Busy (Anyone can Join)
  - Working
  - Do Not Disturb
  - Do Not Approach
- Global status applies to all groups
- Group-specific statuses override global status
- "Last write wins" priority logic

### Group Management
- Create groups with custom colors and profile pictures
- Generate shareable invite links
- Add members via username search
- Leave or remove members
- Admin controls for group settings

### Navigation
- Dashboard (home screen)
- Right-hand navigation bar (collapsible)
- Global Couch Icon ?? for navigation
- Group pages with themed colors

## API Documentation

See [backend/README.md](./backend/README.md) for detailed API endpoint documentation.

### Authentication

Most endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

Tokens are obtained via `/api/auth/signin` or `/api/auth/signup`.

## Database Schema

The SQLite database includes:
- **users** - User accounts with encrypted passwords
- **groups** - Groups with custom colors and invite codes
- **group_members** - Group membership with roles
- **user_groups** - Quick lookup for user's groups
- **invitations** - Group invitations with status tracking

## Development Guidelines

- Follow Material Design 3 guidelines for UI
- Use Kotlin coroutines for async operations
- Implement proper error handling
- Use Compose state management best practices
- Write unit tests for ViewModels
- Follow the MVVM architecture pattern
- Backend follows RESTful API conventions

## Production Considerations

For production deployment (<100 users):
- Keep SQLite for simplicity
- Consider adding database backups
- Use environment variables for sensitive config
- Set up HTTPS/SSL certificates
- Implement rate limiting
- Add request logging and monitoring

For scaling beyond 100 users:
- Consider migrating to PostgreSQL
- Add Redis for caching
- Implement WebSocket for real-time updates
- Add load balancing if needed

## Resources

- [Design Document](./Sock%20on%20the%20Door%20App.pdf) - Complete MVP specifications
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Ktor Documentation](https://ktor.io/)
- [Express.js Documentation](https://expressjs.com/)

## License

[To be determined]

## Contributing

[To be determined]
