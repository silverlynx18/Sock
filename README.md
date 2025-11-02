# Sock App

A social availability app for Android that helps friends and groups coordinate in-person hangouts by sharing real-time availability status.

## Overview

"Sock" is inspired by the simple concept of a "sock on the door" - a universally understood signal of availability. The app allows users to:
- Set global or group-specific status to indicate availability
- Create and manage groups of friends
- View who's available within their groups
- Join groups via invite links

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Backend**: Firebase
  - Authentication (Email/Password)
  - Firestore (Database)
  - Cloud Functions (Server-side logic)
  - Storage (Profile pictures)
- **Design**: Material Design 3

## Project Structure

```
app/
??? src/main/java/com/sock/app/
?   ??? data/
?   ?   ??? model/          # Data models (User, Group, Invitation, Status)
?   ?   ??? repository/     # Data access layer (Firebase repositories)
?   ??? ui/
?   ?   ??? navigation/     # Navigation setup
?   ?   ??? screens/        # UI screens (Login, Dashboard, Groups, etc.)
?   ?   ??? theme/          # App theme and styling
?   ??? MainActivity.kt     # Main entry point
```

## Current Status

### ? Completed
- [x] Project structure and Gradle configuration
- [x] Core data models (User, Group, Invitation, Status)
- [x] Firebase repositories (Auth, User, Group, Invitation, Functions)
- [x] Basic navigation setup
- [x] Authentication screens (Login, Sign Up) - UI only
- [x] Dashboard screen placeholder

### ?? In Progress
- [ ] Firebase configuration setup
- [ ] Authentication logic implementation
- [ ] Dashboard with status system
- [ ] Group management screens
- [ ] Invitations system

### ?? TODO
- [ ] Implement ViewModels for all screens
- [ ] Complete Dashboard with Global Status and Group Elements
- [ ] Create Group Page and Group Details screens
- [ ] Implement Status selection UI
- [ ] Create Manage Groups screen
- [ ] Implement invitation acceptance flow
- [ ] Add right-hand navigation bar
- [ ] Implement Cloud Functions
- [ ] Set up Firebase Security Rules
- [ ] Add profile picture upload functionality
- [ ] Implement user profile screens

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Firebase project account

### Initial Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd sock-app
   ```

2. **Set up Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication (Email/Password)
   - Create a Firestore database
   - Enable Cloud Functions
   - Enable Storage
   - Download `google-services.json` and place it in `app/` directory

3. **Open in Android Studio**
   - Open the project in Android Studio
   - Sync Gradle files
   - Let Android Studio download dependencies

4. **Configure Firebase Cloud Functions**
   - See `functions/` directory (to be created) for Cloud Functions setup
   - Deploy functions using Firebase CLI:
     ```bash
     firebase deploy --only functions
     ```

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

## Firebase Data Structure

### Users Collection (`/users/{userId}`)
```json
{
  "uid": "string",
  "username": "string (unique)",
  "displayName": "string",
  "email": "string",
  "phoneNumber": "string",
  "profilePictureUrl": "string?",
  "createdAt": "timestamp",
  "globalStatusId": "string?",
  "groupSpecificStatuses": {
    "groupId": "statusId"
  },
  "groups": ["groupId1", "groupId2"]
}
```

### Groups Collection (`/groups/{groupId}`)
```json
{
  "groupId": "string",
  "name": "string",
  "groupProfilePictureUrl": "string?",
  "primaryColor": "string",
  "secondaryColor": "string",
  "createdAt": "timestamp",
  "ownerId": "string",
  "inviteLinkCode": "string (unique)",
  "members": {
    "userId": {
      "role": "owner|admin|member",
      "username": "string",
      "joinedAt": "timestamp"
    }
  }
}
```

### Invitations Collection (`/invitations/{invitationId}`)
```json
{
  "groupID": "string",
  "groupName": "string",
  "invitedUserID": "string",
  "inviterUserID": "string",
  "status": "pending_acceptance|accepted|declined",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

## Cloud Functions

The app requires the following Cloud Functions (see design document for details):
1. `checkUsernameAvailability` - Validates username uniqueness
2. `createGroup` - Creates a new group with invite link
3. `processInviteLink` - Processes invite link clicks
4. `acceptInvitation` - Accepts a group invitation
5. `declineInvitation` - Declines a group invitation
6. `handleUserLeaveOrRemove` - Handles user leaving/removal
7. `transferOwnership` - Transfers group ownership
8. `deleteEmptyGroup` - Deletes empty groups
9. `processBlindUsernameInvite` - Invites user by username
10. `deleteUserAccount` - Deletes user account and cleanup

## Development Guidelines

- Follow Material Design 3 guidelines
- Use Kotlin coroutines for async operations
- Implement proper error handling
- Use Compose state management best practices
- Write unit tests for ViewModels
- Follow the MVVM architecture pattern

## Resources

- [Design Document](./Sock%20on%20the%20Door%20App.pdf) - Complete MVP specifications
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Firebase Documentation](https://firebase.google.com/docs)

## License

[To be determined]

## Contributing

[To be determined]
