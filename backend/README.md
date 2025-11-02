# Sock Backend API

REST API backend for the Sock app, built with Node.js, Express, and SQLite.

## Setup

1. **Install dependencies**
   ```bash
   cd backend
   npm install
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and set your JWT_SECRET
   ```

3. **Initialize database**
   ```bash
   npm run init-db
   # Or it will auto-initialize on first server start
   ```

4. **Start server**
   ```bash
   npm start
   # Or for development with auto-reload:
   npm run dev
   ```

The API will be available at `http://localhost:3000/api`

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Create new user account
- `POST /api/auth/signin` - Sign in user

### Users
- `GET /api/users/:userId` - Get user by ID
- `GET /api/users/check-username/:username` - Check username availability
- `PATCH /api/users/:userId` - Update user
- `GET /api/users/:userId/groups` - Get user's groups
- `DELETE /api/users/:userId` - Delete user account

### Groups
- `POST /api/groups` - Create new group
- `GET /api/groups/:groupId` - Get group by ID
- `PATCH /api/groups/:groupId` - Update group (admin/owner only)

### Invitations
- `GET /api/invitations/pending/:userId` - Get pending invitations
- `POST /api/invitations/process-link` - Process invite link code
- `POST /api/invitations/:invitationId/accept` - Accept invitation
- `POST /api/invitations/:invitationId/decline` - Decline invitation

## Authentication

Most endpoints require authentication via JWT token. Include the token in the Authorization header:
```
Authorization: Bearer <token>
```

## Database Schema

- **users** - User accounts
- **groups** - Groups
- **group_members** - Group membership (many-to-many)
- **user_groups** - Quick lookup table for user's groups
- **invitations** - Group invitations

## Notes

- The database file (`sock.db`) is created automatically on first run
- For production, consider using PostgreSQL instead of SQLite
- Update `JWT_SECRET` in `.env` for production
- API expects JSON request bodies and returns JSON responses
