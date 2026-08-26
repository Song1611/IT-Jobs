import { apiGet, apiPut, apiPost, apiDelete, apiPatch, apiGetPaginated } from "./api";

const ENDPOINT = "/api/users";

function getToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export const userApi = {
  // Lấy danh sách tất cả users (admin)
  getAll: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  // Lấy thông tin user theo ID
  getById: (id) => {
    return apiGet(`${ENDPOINT}/${id}`);
  },

  // Cập nhật thông tin user
  update: (id, data) => {
    return apiPut(
      `${ENDPOINT}/${id}`,
      data
    );
  },

  // Lấy các bài đăng của user
  getPosts: (
  id,
  pageNumber = 1,
  pageSize = 10) =>
  {
    return apiGetPaginated(
      `/api/posts/user/${id}`,
      pageNumber,
      pageSize
    );
  },

  // Lấy danh sách skill của một user
  getUserSkills: (userId) => {
    return apiGet(`${ENDPOINT}/${userId}/skills`);
  },

  // Cập nhật avatar
  updateAvatar: async (id, file) => {
    const formData = new FormData();
    formData.append("avatar", file);

    const token = getToken();

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${id}/avatar`,
      {
        method: "PUT",
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: formData
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || "Failed to update avatar");
    }

    return response.json();
  },

  // Cập nhật ảnh bìa
  updateCoverImage: async (id, file) => {
    const formData = new FormData();
    formData.append("coverImage", file);

    const token = getToken();

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${id}/cover-image`,
      {
        method: "PUT",
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: formData
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || "Failed to update cover image");
    }

    return response.json();
  },

  // Cập nhật CV
  updateCV: async (id, file) => {
    const formData = new FormData();
    formData.append("cv", file);

    const token = getToken();

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${id}/cv`,
      {
        method: "PUT",
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: formData
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || "Failed to update CV");
    }

    return response.json();
  },

  // Đổi mật khẩu
  changePassword: (id, currentPassword, newPassword, otp) => {
    return apiPost(
      `${ENDPOINT}/${id}/change-password`,
      { currentPassword, newPassword, otp }
    );
  },

  // Gửi OTP xác nhận trước khi đổi mật khẩu
  sendChangePasswordOtp: (id) => {
    return apiPost(`${ENDPOINT}/${id}/send-change-password-otp`);
  },

  // Lấy media (ảnh/video) của user
  getMedia: (
  id,
  pageNumber = 1,
  pageSize = 6) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${id}/media`,
      pageNumber,
      pageSize
    );
  },

  // Lấy skill của người đăng nhập
  getMySkills: () => {
    return apiGet(`${ENDPOINT}/me/skills`);
  },

  // Thêm skill cho người đăng nhập
  addMySkill: (data) => {
    return apiPost(`${ENDPOINT}/me/skills`, data);
  },

  // Cập nhật thông tin skill (level, years of experience...)
  updateMySkill: (skillId, data) => {
    return apiPatch(`${ENDPOINT}/me/skills/${skillId}`, data);
  },

  // Xóa skill của người đăng nhập
  removeMySkill: (skillId) => {
    return apiDelete(`${ENDPOINT}/me/skills/${skillId}`);
  },

  // Xóa user (admin only - cascade delete)
  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  }
};