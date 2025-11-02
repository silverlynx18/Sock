import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Change this to your backend URL
const API_URL = __DEV__ ? 'http://localhost:3000/api' : 'https://your-production-api.com/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use(
  async (config) => {
    const token = await AsyncStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Auth endpoints
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  getMe: () => api.get('/auth/me'),
};

// Status endpoints
export const statusAPI = {
  updateStatus: (data) => api.put('/status', data),
  getHistory: () => api.get('/status/history'),
  getRoommateStatuses: () => api.get('/status/roommates'),
};

// Roommate endpoints
export const roommateAPI = {
  search: (query) => api.get(`/roommates/search?query=${query}`),
  getRoommates: () => api.get('/roommates'),
  addRoommate: (userId) => api.post(`/roommates/${userId}`),
  removeRoommate: (userId) => api.delete(`/roommates/${userId}`),
};

export default api;
