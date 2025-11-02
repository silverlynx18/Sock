module.exports = function initDatabase(db) {
    // Users table
    db.serialize(() => {
        db.run(`CREATE TABLE IF NOT EXISTS users (
            uid TEXT PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            displayName TEXT NOT NULL,
            email TEXT UNIQUE NOT NULL,
            phoneNumber TEXT,
            passwordHash TEXT NOT NULL,
            profilePictureUrl TEXT,
            createdAt INTEGER NOT NULL,
            globalStatusId TEXT,
            groupSpecificStatuses TEXT DEFAULT '{}'
        )`);

        // Groups table
        db.run(`CREATE TABLE IF NOT EXISTS groups (
            groupId TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            groupProfilePictureUrl TEXT,
            primaryColor TEXT DEFAULT '#6200EE',
            secondaryColor TEXT DEFAULT '#03DAC6',
            createdAt INTEGER NOT NULL,
            ownerId TEXT NOT NULL,
            inviteLinkCode TEXT UNIQUE NOT NULL,
            FOREIGN KEY (ownerId) REFERENCES users(uid)
        )`);

        // Group members (many-to-many relationship)
        db.run(`CREATE TABLE IF NOT EXISTS group_members (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            groupId TEXT NOT NULL,
            userId TEXT NOT NULL,
            role TEXT DEFAULT 'member',
            username TEXT NOT NULL,
            joinedAt INTEGER NOT NULL,
            FOREIGN KEY (groupId) REFERENCES groups(groupId),
            FOREIGN KEY (userId) REFERENCES users(uid),
            UNIQUE(groupId, userId)
        )`);

        // User groups (for quick lookup)
        db.run(`CREATE TABLE IF NOT EXISTS user_groups (
            userId TEXT NOT NULL,
            groupId TEXT NOT NULL,
            PRIMARY KEY (userId, groupId),
            FOREIGN KEY (userId) REFERENCES users(uid),
            FOREIGN KEY (groupId) REFERENCES groups(groupId)
        )`);

        // Invitations table
        db.run(`CREATE TABLE IF NOT EXISTS invitations (
            invitationId TEXT PRIMARY KEY,
            groupId TEXT NOT NULL,
            groupName TEXT NOT NULL,
            invitedUserID TEXT NOT NULL,
            inviterUserID TEXT NOT NULL,
            status TEXT DEFAULT 'pending_acceptance',
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER,
            FOREIGN KEY (groupId) REFERENCES groups(groupId),
            FOREIGN KEY (invitedUserID) REFERENCES users(uid),
            FOREIGN KEY (inviterUserID) REFERENCES users(uid)
        )`);

        // Create indexes for better performance
        db.run(`CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_group_members_userId ON group_members(userId)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_group_members_groupId ON group_members(groupId)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_invitations_invitedUserID ON invitations(invitedUserID)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_invitations_status ON invitations(status)`);

        console.log('Database initialized successfully');
    });
};
