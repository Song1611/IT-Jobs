import { apiPost, apiGet } from './api';

const ENDPOINT = '/api/auth';

export const authApi = {
  // Đăng ký (legacy/user)
  registerUser: async (data) => {
    const response = await apiPost(`${ENDPOINT}/register`, data);
    // Giả sử API trả về luôn token hoặc cần login lại
    if (response.accessToken) {
       const userResponse = await apiGet('/api/users/my-info', { token: response.accessToken });
       return { success: true, data: { accessToken: response.accessToken, refreshToken: response.refreshToken, user: userResponse } };
    }
    return { success: true, data: response };
  },

  // Đăng ký nhà tuyển dụng
  registerHR: async (data) => {
    const response = await apiPost(`${ENDPOINT}/register-hr`, data);
    if (response.accessToken) {
       const userResponse = await apiGet('/api/users/my-info', { token: response.accessToken });
       // Company info is fetched by provider, or we could fetch it here
       return { success: true, data: { accessToken: response.accessToken, refreshToken: response.refreshToken, user: userResponse } };
    }
    return { success: true, data: response };
  },

  // Đăng nhập
  login: async (data) => {
    // data có dạng { email, password }, backend cần { username, password }
    const payload = {
      username: data.email || data.username,
      password: data.password
    };
    const response = await apiPost(`${ENDPOINT}/login`, payload);
    
    if (response && response.accessToken) {
      // Explicitly pass token because it's not in localStorage yet
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

  // Làm mới token
  refreshToken: async (refreshToken) => {
    const response = await apiPost(`${ENDPOINT}/refresh`, { refreshToken });
    
    if (response && response.accessToken) {
       const userResponse = await apiGet('/api/users/my-info', { token: response.accessToken });
       return { success: true, data: { accessToken: response.accessToken, refreshToken: response.refreshToken, user: userResponse } };
    }
    return { success: false };
  },

  // Đăng xuất
  logout: () => {
    return apiPost(`${ENDPOINT}/logout`);
  },

  // Lấy thông tin user hiện tại
  getCurrentUser: () => {
    return apiGet(`/api/users/my-info`);
  }
};