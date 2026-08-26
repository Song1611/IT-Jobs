import { apiPost, apiGet } from './api';

const ENDPOINT = '/api/auth';

export const authApi = {
  registerUser: async (data) => {
    const response = await apiPost(`${ENDPOINT}/register`, data);
    return { success: true, data: response };
  },

  registerHR: async (data) => {
    const response = await apiPost(`${ENDPOINT}/register`, data);
    return { success: true, data: response };
  },

  login: async (data) => {
    const payload = {
      username: data.email || data.username,
      password: data.password
    };
    const response = await apiPost(`${ENDPOINT}/login`, payload);
    
    if (response && response.accessToken) {
      const userResponse = await apiGet('/api/users/my-info', { token: response.accessToken });
      return {
        success: true,
        data: {
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          user: userResponse
        }
      };
    }
    return { success: false, message: "No access token received" };
  },

  refreshToken: async () => {
    const response = await apiPost(`${ENDPOINT}/refresh`);
    
    if (response && response.accessToken) {
       const userResponse = await apiGet('/api/users/my-info', { token: response.accessToken });
       return { success: true, data: { accessToken: response.accessToken, refreshToken: response.refreshToken, user: userResponse } };
    }
    return { success: false };
  },

  logout: () => {
    return apiPost(`${ENDPOINT}/logout`);
  },

  getCurrentUser: () => {
    return apiGet(`/api/users/my-info`);
  },

  verifyEmail: (data) => {
    return apiPost(`${ENDPOINT}/verify-email`, data);
  },

  resendOtp: (data) => {
    return apiPost(`${ENDPOINT}/resend-otp`, data);
  },

  forgotPassword: (data) => {
    return apiPost(`${ENDPOINT}/forgot-password`, data);
  },

  resetPassword: (data) => {
    return apiPost(`${ENDPOINT}/reset-password`, data);
  }
};
