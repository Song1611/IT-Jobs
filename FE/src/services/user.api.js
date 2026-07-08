import { apiGet, apiPut, apiPost, apiDelete, apiGetPaginated } from "./api";

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

  // Đổi mật khẩu
  changePassword: (
  id,
  currentPassword,
  newPassword) =>
  {
    return apiPost(
      `${ENDPOINT}/${id}/change-password`,
      { currentPassword, newPassword }
    );
  },

  // Lấy các bài đăng của user
  getPosts: (
  id,
  pageNumber = 1,
  pageSize = 10) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${id}/posts`,
      pageNumber,
      pageSize
    );
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

  // Lấy danh sách ứng tuyển của user
  getApplications: (
  id,
  pageNumber = 1,
  pageSize = 10) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${id}/applications`,
      pageNumber,
      pageSize
    );
  },

  // Lấy kỹ năng của user
  getSkills: (id) => {
    return apiGet(`${ENDPOINT}/${id}/skills`);
  },

  // Thêm kỹ năng cho user
  addSkill: (id, skillId) => {
    return apiPost(
      `${ENDPOINT}/${id}/skills`,
      { skillId }
    );
  },

  // Xóa kỹ năng của user
  removeSkill: (id, skillId) => {
    return apiDelete(`${ENDPOINT}/${id}/skills/${skillId}`);
  },

  // Xóa user (admin only - cascade delete)
  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
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
  }
};