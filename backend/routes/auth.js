const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

module.exports = function authRoutes(db) {
    const router = express.Router();

    // Sign up
    router.post('/signup', async (req, res) => {
        try {
            const { email, password, displayName, username, phoneNumber } = req.body;

            // Validate input
            if (!email || !password || !displayName || !username) {
                return res.status(400).json({ error: 'Missing required fields' });
            }

            // Check if username exists
            db.get('SELECT uid FROM users WHERE username = ?', [username], async (err, row) => {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }
                if (row) {
                    return res.status(400).json({ error: 'Username already exists' });
                }

                // Check if email exists
                db.get('SELECT uid FROM users WHERE email = ?', [email], async (err, row) => {
                    if (err) {
                        return res.status(500).json({ error: 'Database error' });
                    }
                    if (row) {
                        return res.status(400).json({ error: 'Email already exists' });
                    }

                    // Hash password
                    const passwordHash = await bcrypt.hash(password, 10);
                    const uid = uuidv4();
                    const createdAt = Date.now();

                    // Create user
                    db.run(
                        'INSERT INTO users (uid, username, displayName, email, phoneNumber, passwordHash, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)',
                        [uid, username, displayName, email, phoneNumber || '', passwordHash, createdAt],
                        function(err) {
                            if (err) {
                                return res.status(500).json({ error: 'Failed to create user' });
                            }

                            // Generate JWT token
                            const token = jwt.sign(
                                { uid, email, username },
                                process.env.JWT_SECRET || 'your-secret-key',
                                { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
                            );

                            res.json({
                                token,
                                user: {
                                    uid,
                                    username,
                                    displayName,
                                    email,
                                    phoneNumber: phoneNumber || '',
                                    profilePictureUrl: null,
                                    createdAt: new Date(createdAt).toISOString(),
                                    globalStatusId: null,
                                    groupSpecificStatuses: {},
                                    groups: []
                                }
                            });
                        }
                    );
                });
            });
        } catch (error) {
            res.status(500).json({ error: 'Server error' });
        }
    });

    // Sign in
    router.post('/signin', async (req, res) => {
        try {
            const { email, password } = req.body;

            if (!email || !password) {
                return res.status(400).json({ error: 'Email and password required' });
            }

            db.get('SELECT * FROM users WHERE email = ?', [email], async (err, row) => {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }
                if (!row) {
                    return res.status(401).json({ error: 'Invalid credentials' });
                }

                const isValid = await bcrypt.compare(password, row.passwordHash);
                if (!isValid) {
                    return res.status(401).json({ error: 'Invalid credentials' });
                }

                // Get user groups
                db.all('SELECT groupId FROM user_groups WHERE userId = ?', [row.uid], (err, groups) => {
                    if (err) {
                        return res.status(500).json({ error: 'Database error' });
                    }

                    // Parse group-specific statuses
                    let groupSpecificStatuses = {};
                    try {
                        groupSpecificStatuses = JSON.parse(row.groupSpecificStatuses || '{}');
                    } catch (e) {
                        groupSpecificStatuses = {};
                    }

                    // Generate JWT token
                    const token = jwt.sign(
                        { uid: row.uid, email: row.email, username: row.username },
                        process.env.JWT_SECRET || 'your-secret-key',
                        { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
                    );

                    res.json({
                        token,
                        user: {
                            uid: row.uid,
                            username: row.username,
                            displayName: row.displayName,
                            email: row.email,
                            phoneNumber: row.phoneNumber || '',
                            profilePictureUrl: row.profilePictureUrl || null,
                            createdAt: new Date(row.createdAt).toISOString(),
                            globalStatusId: row.globalStatusId || null,
                            groupSpecificStatuses,
                            groups: groups.map(g => g.groupId)
                        }
                    });
                });
            });
        } catch (error) {
            res.status(500).json({ error: 'Server error' });
        }
    });

    return router;
};
