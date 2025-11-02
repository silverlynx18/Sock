const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');

module.exports = function invitationRoutes(db) {
    const router = express.Router();

    // Get pending invitations for user
    router.get('/pending/:userId', authenticateToken, (req, res) => {
        const { userId } = req.params;

        if (req.user.uid !== userId) {
            return res.status(403).json({ error: 'Forbidden' });
        }

        db.all(
            'SELECT * FROM invitations WHERE invitedUserID = ? AND status = ? ORDER BY createdAt DESC',
            [userId, 'pending_acceptance'],
            (err, invitations) => {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }

                res.json(invitations.map(inv => ({
                    invitationId: inv.invitationId,
                    groupId: inv.groupId,
                    groupName: inv.groupName,
                    invitedUserID: inv.invitedUserID,
                    inviterUserID: inv.inviterUserID,
                    status: inv.status,
                    createdAt: new Date(inv.createdAt).toISOString(),
                    updatedAt: inv.updatedAt ? new Date(inv.updatedAt).toISOString() : null
                })));
            }
        );
    });

    // Process invite link
    router.post('/process-link', authenticateToken, (req, res) => {
        const { inviteLinkCode } = req.body;
        const invitedUserUid = req.user.uid;

        if (!inviteLinkCode) {
            return res.status(400).json({ error: 'Invite link code required' });
        }

        // Find group by invite link code
        db.get('SELECT groupId, name FROM groups WHERE inviteLinkCode = ?', [inviteLinkCode], (err, group) => {
            if (err) {
                return res.status(500).json({ error: 'Database error' });
            }
            if (!group) {
                return res.status(404).json({ error: 'Invalid invite link' });
            }

            // Check if user is already a member
            db.get('SELECT userId FROM group_members WHERE groupId = ? AND userId = ?', [group.groupId, invitedUserUid], (err, member) => {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }
                if (member) {
                    return res.status(400).json({ error: 'User is already a member of this group' });
                }

                // Check if invitation already exists
                db.get('SELECT invitationId FROM invitations WHERE groupId = ? AND invitedUserID = ? AND status = ?', 
                    [group.groupId, invitedUserUid, 'pending_acceptance'], 
                    (err, existing) => {
                        if (err) {
                            return res.status(500).json({ error: 'Database error' });
                        }
                        if (existing) {
                            return res.status(400).json({ error: 'Invitation already exists' });
                        }

                        // Create invitation
                        const invitationId = uuidv4();
                        const createdAt = Date.now();

                        db.run(
                            'INSERT INTO invitations (invitationId, groupId, groupName, invitedUserID, inviterUserID, status, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)',
                            [invitationId, group.groupId, group.name, invitedUserUid, group.ownerId, 'pending_acceptance', createdAt],
                            function(err) {
                                if (err) {
                                    return res.status(500).json({ error: 'Failed to create invitation' });
                                }

                                res.json({
                                    invitationId,
                                    groupId: group.groupId,
                                    groupName: group.name,
                                    invitedUserID: invitedUserUid,
                                    inviterUserID: group.ownerId,
                                    status: 'pending_acceptance',
                                    createdAt: new Date(createdAt).toISOString(),
                                    updatedAt: null
                                });
                            }
                        );
                    }
                );
            });
        });
    });

    // Accept invitation
    router.post('/:invitationId/accept', authenticateToken, (req, res) => {
        const { invitationId } = req.params;
        const acceptingUserUid = req.user.uid;

        db.get('SELECT * FROM invitations WHERE invitationId = ?', [invitationId], (err, invitation) => {
            if (err) {
                return res.status(500).json({ error: 'Database error' });
            }
            if (!invitation) {
                return res.status(404).json({ error: 'Invitation not found' });
            }
            if (invitation.invitedUserID !== acceptingUserUid) {
                return res.status(403).json({ error: 'Forbidden' });
            }
            if (invitation.status !== 'pending_acceptance') {
                return res.status(400).json({ error: 'Invitation already processed' });
            }

            const updatedAt = Date.now();

            // Update invitation status
            db.run('UPDATE invitations SET status = ?, updatedAt = ? WHERE invitationId = ?', 
                ['accepted', updatedAt, invitationId], 
                (err) => {
                    if (err) {
                        return res.status(500).json({ error: 'Failed to update invitation' });
                    }

                    // Get user info
                    db.get('SELECT username FROM users WHERE uid = ?', [acceptingUserUid], (err, user) => {
                        if (err || !user) {
                            return res.status(500).json({ error: 'Failed to get user info' });
                        }

                        // Add user to group
                        db.run(
                            'INSERT INTO group_members (groupId, userId, role, username, joinedAt) VALUES (?, ?, ?, ?, ?)',
                            [invitation.groupId, acceptingUserUid, 'member', user.username, updatedAt],
                            (err) => {
                                if (err) {
                                    return res.status(500).json({ error: 'Failed to add user to group' });
                                }

                                // Add to user_groups
                                db.run('INSERT INTO user_groups (userId, groupId) VALUES (?, ?)', 
                                    [acceptingUserUid, invitation.groupId], 
                                    (err) => {
                                        if (err) {
                                            return res.status(500).json({ error: 'Failed to update user groups' });
                                        }

                                        // Update user's groupSpecificStatuses
                                        db.get('SELECT groupSpecificStatuses FROM users WHERE uid = ?', [acceptingUserUid], (err, row) => {
                                            if (!err && row) {
                                                let statuses = {};
                                                try {
                                                    statuses = JSON.parse(row.groupSpecificStatuses || '{}');
                                                } catch (e) {
                                                    statuses = {};
                                                }
                                                statuses[invitation.groupId] = null;
                                                db.run('UPDATE users SET groupSpecificStatuses = ? WHERE uid = ?', 
                                                    [JSON.stringify(statuses), acceptingUserUid]);
                                            }
                                        });

                                        res.json({ message: 'Invitation accepted successfully' });
                                    }
                                );
                            }
                        );
                    });
                }
            );
        });
    });

    // Decline invitation
    router.post('/:invitationId/decline', authenticateToken, (req, res) => {
        const { invitationId } = req.params;
        const decliningUserUid = req.user.uid;

        db.get('SELECT * FROM invitations WHERE invitationId = ?', [invitationId], (err, invitation) => {
            if (err) {
                return res.status(500).json({ error: 'Database error' });
            }
            if (!invitation) {
                return res.status(404).json({ error: 'Invitation not found' });
            }
            if (invitation.invitedUserID !== decliningUserUid) {
                return res.status(403).json({ error: 'Forbidden' });
            }

            const updatedAt = Date.now();

            db.run('UPDATE invitations SET status = ?, updatedAt = ? WHERE invitationId = ?', 
                ['declined', updatedAt, invitationId], 
                (err) => {
                    if (err) {
                        return res.status(500).json({ error: 'Failed to decline invitation' });
                    }
                    res.json({ message: 'Invitation declined' });
                }
            );
        });
    });

    return router;
};
