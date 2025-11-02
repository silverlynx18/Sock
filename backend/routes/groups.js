const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');

module.exports = function groupRoutes(db) {
    const router = express.Router();

    // Create group
    router.post('/', authenticateToken, (req, res) => {
        const { groupName, primaryColor, secondaryColor, groupProfilePictureUrl } = req.body;
        const creatorUid = req.user.uid;
        const creatorUsername = req.user.username;

        if (!groupName) {
            return res.status(400).json({ error: 'Group name required' });
        }

        const groupId = uuidv4();
        const inviteLinkCode = uuidv4().replace(/-/g, '').substring(0, 12);
        const createdAt = Date.now();

        db.run(
            'INSERT INTO groups (groupId, name, groupProfilePictureUrl, primaryColor, secondaryColor, createdAt, ownerId, inviteLinkCode) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
            [groupId, groupName, groupProfilePictureUrl || null, primaryColor || '#6200EE', secondaryColor || '#03DAC6', createdAt, creatorUid, inviteLinkCode],
            function(err) {
                if (err) {
                    return res.status(500).json({ error: 'Failed to create group' });
                }

                // Add creator as owner
                db.run(
                    'INSERT INTO group_members (groupId, userId, role, username, joinedAt) VALUES (?, ?, ?, ?, ?)',
                    [groupId, creatorUid, 'owner', creatorUsername, createdAt],
                    (err) => {
                        if (err) {
                            return res.status(500).json({ error: 'Failed to add creator to group' });
                        }

                        // Add to user_groups
                        db.run(
                            'INSERT INTO user_groups (userId, groupId) VALUES (?, ?)',
                            [creatorUid, groupId],
                            (err) => {
                                if (err) {
                                    return res.status(500).json({ error: 'Failed to update user groups' });
                                }

                                // Update user's groupSpecificStatuses
                                db.get('SELECT groupSpecificStatuses FROM users WHERE uid = ?', [creatorUid], (err, row) => {
                                    if (!err && row) {
                                        let statuses = {};
                                        try {
                                            statuses = JSON.parse(row.groupSpecificStatuses || '{}');
                                        } catch (e) {
                                            statuses = {};
                                        }
                                        statuses[groupId] = null;
                                        db.run('UPDATE users SET groupSpecificStatuses = ? WHERE uid = ?', [JSON.stringify(statuses), creatorUid]);
                                    }
                                });

                                res.json({
                                    groupId,
                                    inviteLinkCode
                                });
                            }
                        );
                    }
                );
            }
        );
    });

    // Get group by ID
    router.get('/:groupId', authenticateToken, (req, res) => {
        const { groupId } = req.params;

        db.get('SELECT * FROM groups WHERE groupId = ?', [groupId], (err, group) => {
            if (err) {
                return res.status(500).json({ error: 'Database error' });
            }
            if (!group) {
                return res.status(404).json({ error: 'Group not found' });
            }

            // Get members
            db.all('SELECT userId, role, username, joinedAt FROM group_members WHERE groupId = ?', [groupId], (err, members) => {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }

                const membersMap = {};
                members.forEach(member => {
                    membersMap[member.userId] = {
                        role: member.role,
                        username: member.username,
                        joinedAt: new Date(member.joinedAt).toISOString()
                    };
                });

                res.json({
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
            });
        });
    });

    // Update group
    router.patch('/:groupId', authenticateToken, (req, res) => {
        const { groupId } = req.params;
        const updates = req.body;

        // Verify user is admin/owner
        db.get('SELECT role FROM group_members WHERE groupId = ? AND userId = ?', [groupId, req.user.uid], (err, member) => {
            if (err || !member || (member.role !== 'owner' && member.role !== 'admin')) {
                return res.status(403).json({ error: 'Forbidden' });
            }

            const allowedFields = ['name', 'primaryColor', 'secondaryColor', 'groupProfilePictureUrl'];
            const updateFields = [];
            const values = [];

            for (const field of allowedFields) {
                if (updates[field] !== undefined) {
                    updateFields.push(`${field} = ?`);
                    values.push(updates[field]);
                }
            }

            if (updateFields.length === 0) {
                return res.status(400).json({ error: 'No valid fields to update' });
            }

            values.push(groupId);

            db.run(`UPDATE groups SET ${updateFields.join(', ')} WHERE groupId = ?`, values, (err) => {
                if (err) {
                    return res.status(500).json({ error: 'Failed to update group' });
                }

                // Return updated group
                db.get('SELECT * FROM groups WHERE groupId = ?', [groupId], (err, group) => {
                    if (err || !group) {
                        return res.status(500).json({ error: 'Failed to fetch updated group' });
                    }

                    db.all('SELECT userId, role, username, joinedAt FROM group_members WHERE groupId = ?', [groupId], (err, members) => {
                        if (err) {
                            return res.status(500).json({ error: 'Database error' });
                        }

                        const membersMap = {};
                        members.forEach(member => {
                            membersMap[member.userId] = {
                                role: member.role,
                                username: member.username,
                                joinedAt: new Date(member.joinedAt).toISOString()
                            };
                        });

                        res.json({
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
                    });
                });
            });
        });
    });

    return router;
};
