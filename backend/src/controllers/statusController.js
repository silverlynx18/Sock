const User = require('../models/User');
const StatusHistory = require('../models/StatusHistory');
const Notification = require('../models/Notification');

// @desc    Update user status
// @route   PUT /api/status
// @access  Private
const updateStatus = async (req, res) => {
  try {
    const { status, statusMessage } = req.body;

    const user = await User.findById(req.user._id);

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Save to history
    await StatusHistory.create({
      user: user._id,
      status: user.status,
      statusMessage: user.statusMessage,
      timestamp: user.lastStatusChange,
    });

    // Update user status
    user.status = status || user.status;
    user.statusMessage = statusMessage !== undefined ? statusMessage : user.statusMessage;
    user.lastStatusChange = new Date();

    await user.save();

    // Notify roommates
    if (user.roommates && user.roommates.length > 0) {
      const notifications = user.roommates.map(roommateId => ({
        recipient: roommateId,
        sender: user._id,
        type: 'statusChange',
        title: 'Roommate Status Update',
        body: `${user.name} is now ${status}${statusMessage ? ': ' + statusMessage : ''}`,
        data: {
          status,
          statusMessage,
          userId: user._id
        }
      }));

      await Notification.insertMany(notifications);
    }

    // Emit socket event (if using Socket.IO)
    if (req.app.get('io')) {
      user.roommates.forEach(roommateId => {
        req.app.get('io').to(roommateId.toString()).emit('statusUpdate', {
          userId: user._id,
          name: user.name,
          status: user.status,
          statusMessage: user.statusMessage
        });
      });
    }

    res.json({
      status: user.status,
      statusMessage: user.statusMessage,
      lastStatusChange: user.lastStatusChange
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Get status history
// @route   GET /api/status/history
// @access  Private
const getStatusHistory = async (req, res) => {
  try {
    const history = await StatusHistory.find({ user: req.user._id })
      .sort({ timestamp: -1 })
      .limit(50);

    res.json(history);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Get roommate statuses
// @route   GET /api/status/roommates
// @access  Private
const getRoommateStatuses = async (req, res) => {
  try {
    const user = await User.findById(req.user._id).populate(
      'roommates',
      'name email status statusMessage lastStatusChange'
    );

    res.json(user.roommates);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  updateStatus,
  getStatusHistory,
  getRoommateStatuses,
};
