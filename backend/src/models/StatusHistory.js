const mongoose = require('mongoose');

const statusHistorySchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  status: {
    type: String,
    enum: ['available', 'busy', 'doNotDisturb', 'away'],
    required: true
  },
  statusMessage: {
    type: String,
    maxlength: 200
  },
  timestamp: {
    type: Date,
    default: Date.now
  },
  duration: {
    type: Number // Duration in minutes
  }
});

// Index for efficient querying
statusHistorySchema.index({ user: 1, timestamp: -1 });

module.exports = mongoose.model('StatusHistory', statusHistorySchema);
