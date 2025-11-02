# ?? Sock on the Door App

A modern mobile application for college roommates to communicate their availability status in real-time. Never wonder if it's a good time to head back to your dorm room again!

## ?? Features

- **Real-time Status Updates**: Set your status to Available, Busy, Do Not Disturb, or Away
- **Custom Status Messages**: Add personalized messages to your status
- **Roommate Management**: Connect with your roommates and see their statuses instantly
- **Push Notifications**: Get notified when your roommates change their status
- **User Authentication**: Secure JWT-based authentication system
- **Room & Dorm Information**: Keep track of room numbers and dorm locations

## ??? Project Structure

```
/workspace
??? backend/                 # Node.js + Express API
?   ??? src/
?   ?   ??? config/         # Database configuration
?   ?   ??? controllers/    # Route controllers
?   ?   ??? models/         # MongoDB models
?   ?   ??? routes/         # API routes
?   ?   ??? middleware/     # Authentication middleware
?   ?   ??? server.js       # Express server setup
?   ??? package.json
?
??? mobile/                 # React Native + Expo app
    ??? src/
    ?   ??? screens/        # App screens
    ?   ??? navigation/     # Navigation configuration
    ?   ??? services/       # API service layer
    ?   ??? context/        # React Context (Auth)
    ?   ??? components/     # Reusable components
    ??? App.js
    ??? package.json
```

## ?? Getting Started

### Prerequisites

- Node.js (v16 or higher)
- MongoDB (local or MongoDB Atlas)
- Expo CLI: `npm install -g expo-cli`
- iOS Simulator or Android Emulator (or Expo Go app on your phone)

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Create a `.env` file based on `.env.example`:
   ```bash
   cp .env.example .env
   ```

4. Update the `.env` file with your configuration:
   ```
   PORT=3000
   MONGODB_URI=mongodb://localhost:27017/sock-on-door
   JWT_SECRET=your_secure_secret_key_here
   NODE_ENV=development
   ```

5. Start MongoDB (if running locally):
   ```bash
   mongod
   ```

6. Start the backend server:
   ```bash
   npm run dev
   ```

   The API will be available at `http://localhost:3000`

### Mobile App Setup

1. Navigate to the mobile directory:
   ```bash
   cd mobile
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Update the API URL in `src/services/api.js`:
   - For iOS Simulator: `http://localhost:3000/api`
   - For Android Emulator: `http://10.0.2.2:3000/api`
   - For physical device: Use your computer's IP address

4. Start the Expo development server:
   ```bash
   npm start
   ```

5. Run on your device:
   - Scan the QR code with Expo Go app (iOS/Android)
   - Press `i` for iOS Simulator
   - Press `a` for Android Emulator

## ?? API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/me` - Get current user (protected)

### Status Management
- `PUT /api/status` - Update user status (protected)
- `GET /api/status/history` - Get status history (protected)
- `GET /api/status/roommates` - Get roommates' statuses (protected)

### Roommate Management
- `GET /api/roommates` - Get all roommates (protected)
- `GET /api/roommates/search?query=` - Search users (protected)
- `POST /api/roommates/:userId` - Add a roommate (protected)
- `DELETE /api/roommates/:userId` - Remove a roommate (protected)

## ?? Status Types

- **Available** ? - Ready for visitors or chat
- **Busy** ?? - Currently occupied, knock before entering
- **Do Not Disturb** ?? - Need privacy, please come back later
- **Away** ?? - Not in the room

## ?? Security Features

- Password hashing with bcryptjs
- JWT-based authentication
- Protected API routes
- Secure token storage on mobile device

## ?? Tech Stack

### Backend
- **Node.js** - JavaScript runtime
- **Express.js** - Web framework
- **MongoDB** - NoSQL database
- **Mongoose** - MongoDB ODM
- **Socket.IO** - Real-time communication
- **JWT** - Authentication tokens
- **bcryptjs** - Password hashing

### Mobile
- **React Native** - Mobile framework
- **Expo** - Development platform
- **React Navigation** - Navigation library
- **React Native Paper** - Material Design components
- **Axios** - HTTP client
- **AsyncStorage** - Local data persistence

## ?? Testing

### Backend
```bash
cd backend
npm test
```

### Mobile
The mobile app can be tested using Expo's testing tools or by running on actual devices/emulators.

## ?? Environment Variables

### Backend `.env`
```
PORT=3000
MONGODB_URI=mongodb://localhost:27017/sock-on-door
JWT_SECRET=your_jwt_secret_key_here_change_in_production
NODE_ENV=development
```

## ?? Contributing

This is a college dorm communication app. Contributions, issues, and feature requests are welcome!

## ?? License

MIT License - feel free to use this project for your own dorm or campus!

## ?? Future Enhancements

- [ ] Push notifications for status changes
- [ ] Status scheduling (automatic status changes)
- [ ] Group rooms for suites or apartments
- [ ] Status analytics and insights
- [ ] Custom status options
- [ ] Photo profiles
- [ ] In-app messaging
- [ ] Status expiration timers

## ?? How to Use

1. **Create an Account**: Sign up with your name, email, and dorm information
2. **Add Roommates**: Search for your roommates by email or name and connect
3. **Set Your Status**: Choose from Available, Busy, Do Not Disturb, or Away
4. **Add a Message**: Optionally add a custom message like "Studying for finals"
5. **Stay Informed**: Check your roommates' statuses before heading home!

## ?? Support

For questions or issues, please open an issue in the repository.

---

Made with ?? for college students everywhere
