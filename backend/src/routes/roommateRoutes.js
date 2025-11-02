const express = require('express');
const router = express.Router();
const {
  searchUsers,
  addRoommate,
  removeRoommate,
  getRoommates,
} = require('../controllers/roommateController');
const { protect } = require('../middleware/auth');

router.get('/search', protect, searchUsers);
router.get('/', protect, getRoommates);
router.post('/:userId', protect, addRoommate);
router.delete('/:userId', protect, removeRoommate);

module.exports = router;
