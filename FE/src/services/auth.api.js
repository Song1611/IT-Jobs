import { apiPost } from './api';










const ENDPOINT = '/api/Auth';

export const authApi = {
  // Đăng ký ứng viên (role = user)
  registerUser: async (data) => {
    const response = await apiPost(`${ENDPOINT}/register-user`, data);

    // Lưu userInfo vào localStorage sau khi đăng ký thành công
    if (response.data?.user) {
      localStorage.setItem('userInfo', JSON.stringify(response.data.user));
    }

    return response;
  },

  // Đăng ký nhà tuyển dụng (role = employer)
  registerHR: async (data) => {
    const response = await apiPost(`${ENDPOINT}/register-hr`, data);

    // Lưu userInfo vào localStorage sau khi đăng ký thành công
    if (response.data?.user) {
      localStorage.setItem('userInfo', JSON.stringify(response.data.user));
    }

    return response;
  },

  // Đăng ký (legacy)
  register: async (data) => {
    const response = await apiPost(`${ENDPOINT}/register`, data);

    // Lưu userInfo vào localStorage
    if (response.data?.user) {
      localStorage.setItem('userInfo', JSON.stringify(response.data.user));
    }

    return response;
  },

  // Đăng nhập
  login: async (data) => {
    const response = await apiPost(`${ENDPOINT}/login`, data);

    // Lưu userInfo vào localStorage sau khi login thành công
    if (response.data?.user) {
      localStorage.setItem('userInfo', JSON.stringify(response.data.user));
    }

    return response;
  },

  // Làm mới token
  refreshToken: async (refreshToken) => {
    const response = await apiPost(`${ENDPOINT}/refresh-token`, {
      refreshToken
    });

    // Cập nhật userInfo nếu có
    if (response.data?.user) {
      localStorage.setItem('userInfo', JSON.stringify(response.data.user));
    }

    return response;
  },

  // Đăng xuất
  logout: (token) => {
    // Xóa userInfo khi logout
    localStorage.removeItem('userInfo');
    return apiPost(`${ENDPOINT}/logout`, undefined, { token });
  },

  // Lấy thông tin user hiện tại
  getCurrentUser: (token) => {
    return apiPost(`${ENDPOINT}/me`, undefined, { token });
  }
};