import { apiGet, apiPut, apiPost, apiDelete, apiGetPaginated } from "./api";






const ENDPOINT = "/api/User";

export const userApi = {
  // Lấy danh sách tất cả users (admin)
  getAll: (pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, { token });
  },

  // Lấy thông tin user theo ID
  getById: (id, token) => {
    return apiGet(`${ENDPOINT}/${id}`, { token });
  },

  // Cập nhật thông tin user
  update: (id, data, token) => {
    return apiPut(
      `${ENDPOINT}/${id}`,
      data,
      { token }
    );
  },

  // Cập nhật avatar
  updateAvatar: async (id, file, token) => {
    const formData = new FormData();
    formData.append("avatar", file);

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${id}/avatar`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`
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
  updateCoverImage: async (id, file, token) => {
    const formData = new FormData();
    formData.append("coverImage", file);

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${id}/cover-image`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`
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
  newPassword,
  token) =>
  {
    return apiPost(
      `${ENDPOINT}/${id}/change-password`,
      { currentPassword, newPassword },
      { token }
    );
  },

  // Lấy các bài đăng của user
  getPosts: (
  id,
  pageNumber = 1,
  pageSize = 10,
  token) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${id}/posts`,
      pageNumber,
      pageSize,
      { token }
    );
  },

  // Lấy media (ảnh/video) của user
  getMedia: (
  id,
  pageNumber = 1,
  pageSize = 6,
  token) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${id}/media`,
      pageNumber,
      pageSize,
      { token }
    );
  },

  // Lấy danh sách ứng tuyển của user
  getApplications: (
  id,
  pageNumber = 1,
  pageSize = 10,
  token) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${id}/applications`,
      pageNumber,
      pageSize,
      { token }
    );
  },

  // Lấy kỹ năng của user
  getSkills: (id, token) => {
    return apiGet(`${ENDPOINT}/${id}/skills`, { token });
  },

  // Thêm kỹ năng cho user
  addSkill: (id, skillId, token) => {
    return apiPost(
      `${ENDPOINT}/${id}/skills`,
      { skillId },
      { token }
    );
  },

  // Xóa kỹ năng của user
  removeSkill: (id, skillId, token) => {
    return apiDelete(`${ENDPOINT}/${id}/skills/${skillId}`, { token });
  },

  // Xóa user (admin only - cascade delete)
  delete: (id, token) => {
    return apiDelete(`${ENDPOINT}/${id}`, { token });
  },

  // Cập nhật CV
  updateCV: async (id, file, token) => {
    const formData = new FormData();
    formData.append("cv", file);

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${id}/cv`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`
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