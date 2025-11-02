# Sock on the Door - Backend API

Node.js backend API for the Sock on the Door application.

## Quick Start

1. Install dependencies:
   ```bash
   npm install
   ```

2. Create `.env` file:
   ```bash
   cp .env.example .env
   ```

3. Start MongoDB

4. Run the server:
   ```bash
   npm run dev
   ```

## API Documentation

See the main README in the root directory for full API documentation.

## Development

- `npm start` - Start production server
- `npm run dev` - Start development server with nodemon
- `npm test` - Run tests

## Database Models

### User
- email, password, name
- roomNumber, dormName
- status, statusMessage
- roommates (array of User IDs)
- pushToken
- timestamps

### StatusHistory
- user (User ID)
- status, statusMessage
- timestamp, duration

### Notification
- recipient, sender (User IDs)
- type, title, body
- data (mixed)
- read status
- timestamp
