# Quick Start Guide - Sock on the Door App

Get the app running in 5 minutes!

## Prerequisites Check

Before starting, make sure you have:
- [ ] Node.js installed (v16+): `node --version`
- [ ] MongoDB installed or access to MongoDB Atlas
- [ ] Expo CLI: `npm install -g expo-cli`
- [ ] A mobile device with Expo Go app OR an emulator

## Step 1: Backend Setup (2 minutes)

```bash
# 1. Navigate to backend
cd backend

# 2. Install dependencies
npm install

# 3. Create environment file
cp .env.example .env

# 4. Edit .env and set your values
# PORT=3000
# MONGODB_URI=mongodb://localhost:27017/sock-on-door
# JWT_SECRET=your_secret_here

# 5. Start MongoDB (if local)
# On Mac: brew services start mongodb-community
# On Linux: sudo systemctl start mongod
# Or use MongoDB Atlas cloud

# 6. Start the server
npm run dev
```

Server should be running at `http://localhost:3000`

## Step 2: Mobile App Setup (2 minutes)

```bash
# 1. Open a new terminal and navigate to mobile
cd mobile

# 2. Install dependencies
npm install

# 3. Update API URL in src/services/api.js
# - For iOS Simulator: http://localhost:3000/api
# - For Android Emulator: http://10.0.2.2:3000/api
# - For Physical Device: http://YOUR_COMPUTER_IP:3000/api

# 4. Start Expo
npm start
```

## Step 3: Run the App (1 minute)

### Option A: Physical Device
1. Install "Expo Go" from App Store or Play Store
2. Scan the QR code that appears in your terminal
3. App will load on your phone!

### Option B: Emulator
- Press `i` for iOS Simulator (Mac only)
- Press `a` for Android Emulator (requires Android Studio)

## Step 4: Test It Out!

1. **Create an account** - Sign up with any email
2. **Set your status** - Try "Busy" with a message
3. **Add roommates** - Search by email or name
4. **See it work** - Open the app on another device or create another account

## Common Issues

### "Cannot connect to server"
- Check backend is running: `http://localhost:3000/health`
- Update API URL in mobile app
- If on physical device, ensure same WiFi network

### "MongoDB connection failed"
- Make sure MongoDB is running
- Check MONGODB_URI in .env
- Try using MongoDB Atlas (free tier available)

### "Expo commands not found"
- Install Expo CLI: `npm install -g expo-cli`
- Or use: `npx expo start`

## Next Steps

- Read the full [README.md](./README.md)
- Check API documentation
- Customize status options
- Add push notifications

## Architecture Overview

```
???????????????????
?  Mobile App     ?
?  (React Native) ?
???????????????????
         ?
         ? HTTP/WebSocket
         ?
???????????????????
?  Backend API    ?
?  (Express.js)   ?
???????????????????
         ?
???????????????????
?  MongoDB        ?
?  (Database)     ?
???????????????????
```

## Development Workflow

1. **Backend Changes**: Server auto-restarts with nodemon
2. **Mobile Changes**: Expo hot-reloads automatically
3. **Database**: Use MongoDB Compass for GUI management

Happy coding! ??
