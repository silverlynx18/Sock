const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,
    trim: true
  },
  password: {
    type: String,
    required: true,
    minlength: 6
  },
  name: {
    type: String,
    required: true,
    trim: true
  },
  roomNumber: {
    type: String,
    trim: true
  },
  dormName: {
    type: String,
    trim: true
  },
  status: {
    type: String,
    enum: ['available', 'busy', 'doNotDisturb', 'away'],
    default: 'available'
  },
  statusMessage: {
    type: String,
    maxlength: 200,
    default: ''
  },
  roommates: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  pushToken: {
    type: String
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  lastStatusChange: {
    type: Date,
    default: Date.now
  }
});

// Hash password before saving
userSchema.pre('save', async function(next) {
  if (!this.isModified('password')) {
    return next();
  }
  
  try {
    const salt = await bcrypt.genSalt(10);
    this.password = await bcrypt.hash(this.password, salt);
    next();
  } catch (error) {
    next(error);
  }
});

// Method to check password
userSchema.methods.comparePassword = async function(candidatePassword) {
  return await bcrypt.compare(candidatePassword, this.password);
};

// Remove password from JSON responses
userSchema.methods.toJSON = function() {
  const obj = this.toObject();
  delete obj.password;
  return obj;
};

module.exports = mongoose.model('User', userSchema);
