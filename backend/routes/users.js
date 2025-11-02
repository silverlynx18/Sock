const express = require('express');
const { authenticateToken } = require('../middleware/auth');

module.exports = function userRoutes(db) {
    const router = express.Router();

    // Get user by ID
    router.get('/:userId', authenticateToken, (req, res) => {
        const { userId } = req.params;

        db.get('SELECT * FROM users WHERE uid = ?', [userId], (err, row) => {
            if (err) {
                return res.status(500).json({ error: 'Database error' });
            }
            if (!row) {
                return res.status(404).json({ error: 'User not found' });
            }

            // Get user groups
            db.all('SELECT groupId FROM user_groups WHERE userId = ?', [userId], (err, groups) => {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }

                let groupSpecificStatuses = {};
                try {
                    groupSpecificStatuses = JSON.parse(row.groupSpecificStatuses || '{}');
                } catch (e) {
                    groupSpecificStatuses = {};
                }

                res.json({
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
                });
            });
        });
    });

    // Check username availability
    router.get('/check-username/:username', authenticateToken, (req, res) => {
        const { username } = req.params;

        db.get('SELECT uid FROM users WHERE username = ?', [username], (err, row) => {
            if (err) {
                return res.status(500).json({ error: 'Database error' });
            }
            res.json({ available: !row });
        });
    });

    // Update user
    router.patch('/:userId', authenticateToken, (req, res) => {
        const { userId } = req.params;

        // Verify user can only update their own profile
        if (req.user.uid !== userId) {
            return res.status(403).json({ error: 'Forbidden' });
        }

        const updates = req.body;
        const allowedFields = ['displayName', 'profilePictureUrl', 'globalStatusId', 'groupSpecificStatuses'];
        const updateFields = [];
        const values = [];

        for (const field of allowedFields) {
            if (updates[field] !== undefined) {
                if (field === 'groupSpecificStatuses') {
                    updateFields.push(`${field} = ?`);
                    values.push(JSON.stringify(updates[field]));
                } else {
                    updateFields.push(`${field} = ?`);
                    values.push(updates[field]);
                }
            }
        }

        if (updateFields.length === 0) {
            return res.status(400).json({ error: 'No valid fields to update' });
        }

        values.push(userId);

        db.run(
            `UPDATE users SET ${updateFields.join(', ')} WHERE uid = ?`,
            values,
            function(err) {
                if (err) {
                    return res.status(500).json({ error: 'Failed to update user' });
                }

                // Return updated user
                db.get('SELECT * FROM users WHERE uid = ?', [userId], (err, row) => {
                    if (err || !row) {
                        return res.status(500).json({ error: 'Failed to fetch updated user' });
                    }

                    db.all('SELECT groupId FROM user_groups WHERE userId = ?', [userId], (err, groups) => {
                        if (err) {
                            return res.status(500).json({ error: 'Database error' });
                        }

                        let groupSpecificStatuses = {};
                        try {
                            groupSpecificStatuses = JSON.parse(row.groupSpecificStatuses || '{}');
                        } catch (e) {
                            groupSpecificStatuses = {};
                        }

                        res.json({
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
                        });
                    });
                });
            }
        );
    });

    // Get user groups
    router.get('/:userId/groups', authenticateToken, (req, res) => {
        const { userId } = req.params;

        db.all(
            `SELECT g.* FROM groups g
             INNER JOIN user_groups ug ON g.groupId = ug.groupId
             WHERE ug.userId = ?`,
            [userId],
            (err, groups) => {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }

                // Get members for each group
                const groupsWithMembers = groups.map(group => {
                    return new Promise((resolve) => {
                        db.all(
                            `SELECT userId, role, username, joinedAt FROM group_members WHERE groupId = ?`,
                            [group.groupId],
                            (err, members) => {
                                if (err) {
                                    resolve({ ...group, members: {} });
                                    return;
                                }

                                const membersMap = {};
                                members.forEach(member => {
                                    membersMap[member.userId] = {
                                        role: member.role,
                                        username: member.username,
                                        joinedAt: new Date(member.joinedAt).toISOString()
                                    };
                                });

                                resolve({
                                    groupId: group.groupId,
                                    name: group.name,
                                    groupProfilePictureUrl: group.groupProfilePictureUrl || null,
                                    primaryColor: group.primaryColor,
                                    secondaryColor: group.secondaryColor,
                                    createdAt: new Date(group.createdAt).toISOString(),
                                    ownerId: group.ownerId,
                                    inviteLinkCode: group.inviteLinkCode,
                                    members: membersMap
                                });
                            }
                        );
                    });
                });

                Promise.all(groupsWithMembers).then(results => {
                    res.json(results);
                });
            }
        );
    });

    // Delete user account
    router.delete('/:userId', authenticateToken, (req, res) => {
        const { userId } = req.params;

        // Verify user can only delete their own account
        if (req.user.uid !== userId) {
            return res.status(403).json({ error: 'Forbidden' });
        }

        // TODO: Implement full cleanup (remove from groups, delete invitations, etc.)
        db.run('DELETE FROM users WHERE uid = ?', [userId], function(err) {
            if (err) {
                return res.status(500).json({ error: 'Failed to delete account' });
            }
            res.json({ message: 'Account deleted successfully' });
        });
    });

    return router;
};
