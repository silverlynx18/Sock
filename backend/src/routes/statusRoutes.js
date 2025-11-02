const express = require('express');
const router = express.Router();
const {
  updateStatus,
  getStatusHistory,
  getRoommateStatuses,
} = require('../controllers/statusController');
const { protect } = require('../middleware/auth');

router.put('/', protect, updateStatus);
router.get('/history', protect, getStatusHistory);
router.get('/roommates', protect, getRoommateStatuses);

module.exports = router;
