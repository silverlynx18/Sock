# Sock on the Door - Mobile App

React Native mobile application built with Expo.

## Quick Start

1. Install dependencies:
   ```bash
   npm install
   ```

2. Update API URL in `src/services/api.js`

3. Start Expo:
   ```bash
   npm start
   ```

4. Run on device:
   - Scan QR code with Expo Go
   - Press `i` for iOS
   - Press `a` for Android

## Features

- User authentication
- Real-time status updates
- Roommate management
- Status history
- Push notifications (when configured)

## Screens

- **Login/Register** - Authentication
- **Home** - Set and view statuses
- **Roommates** - Manage roommate connections
- **Profile** - View profile and logout

## Configuration

Update the API URL in `src/services/api.js` to match your backend:
- Local: `http://localhost:3000/api`
- Android Emulator: `http://10.0.2.2:3000/api`
- Network: `http://YOUR_IP:3000/api`
