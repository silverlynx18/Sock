const User = require('../models/User');
const Notification = require('../models/Notification');

// @desc    Search for users by email or name
// @route   GET /api/roommates/search
// @access  Private
const searchUsers = async (req, res) => {
  try {
    const { query } = req.query;

    if (!query) {
      return res.status(400).json({ message: 'Search query is required' });
    }

    const users = await User.find({
      _id: { $ne: req.user._id }, // Exclude current user
      $or: [
        { email: { $regex: query, $options: 'i' } },
        { name: { $regex: query, $options: 'i' } },
      ],
    })
      .select('name email roomNumber dormName')
      .limit(10);

    res.json(users);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Add a roommate
// @route   POST /api/roommates/:userId
// @access  Private
const addRoommate = async (req, res) => {
  try {
    const { userId } = req.params;

    if (userId === req.user._id.toString()) {
      return res.status(400).json({ message: 'Cannot add yourself as a roommate' });
    }

    const user = await User.findById(req.user._id);
    const roommate = await User.findById(userId);

    if (!roommate) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Check if already roommates
    if (user.roommates.includes(userId)) {
      return res.status(400).json({ message: 'Already roommates' });
    }

    // Add to both users' roommate lists
    user.roommates.push(userId);
    roommate.roommates.push(req.user._id);

    await user.save();
    await roommate.save();

    // Send notification
    await Notification.create({
      recipient: userId,
      sender: req.user._id,
      type: 'roommateAccepted',
      title: 'New Roommate',
      body: `${user.name} is now your roommate`,
      data: { userId: req.user._id }
    });

    res.json({ message: 'Roommate added successfully', roommate });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Remove a roommate
// @route   DELETE /api/roommates/:userId
// @access  Private
const removeRoommate = async (req, res) => {
  try {
    const { userId } = req.params;

    const user = await User.findById(req.user._id);
    const roommate = await User.findById(userId);

    if (!roommate) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Remove from both users' roommate lists
    user.roommates = user.roommates.filter(id => id.toString() !== userId);
    roommate.roommates = roommate.roommates.filter(id => id.toString() !== req.user._id.toString());

    await user.save();
    await roommate.save();

    res.json({ message: 'Roommate removed successfully' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Get all roommates
// @route   GET /api/roommates
// @access  Private
const getRoommates = async (req, res) => {
  try {
    const user = await User.findById(req.user._id).populate(
      'roommates',
      'name email status statusMessage lastStatusChange roomNumber dormName'
    );

    res.json(user.roommates);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  searchUsers,
  addRoommate,
  removeRoommate,
  getRoommates,
};
