import {
  apiPost,
  apiPut,
  apiDelete,
  apiGetPaginated,
  apiGetById,
  apiGet } from
"./api";


const ENDPOINT = "/api/Post";

export const postApi = {
  // Lấy danh sách bài đăng với pagination
  getAll: (
  pageNumber = 1,
  pageSize = 10,
  currentUserId,
  token) =>
  {
    const params = { pageNumber, pageSize };
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGet(ENDPOINT, { params, token });
  },

  // Lấy chi tiết bài đăng
  getById: (id, currentUserId, token) => {
    const params = {};
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGet(`${ENDPOINT}/${id}`, { params, token });
  },

  // Lấy bài đăng theo user
  getByUser: (
  userId,
  pageNumber = 1,
  pageSize = 10,
  currentUserId,
  token) =>
  {
    const params = { pageNumber, pageSize };
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGet(`${ENDPOINT}/user/${userId}`, {
      params,
      token
    });
  },

  // Lấy bài đăng theo company
  getByCompany: (
  companyId,
  pageNumber = 1,
  pageSize = 10,
  currentUserId,
  token) =>
  {
    const params = { pageNumber, pageSize };
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGet(
      `${ENDPOINT}/company/${companyId}`,
      { params, token }
    );
  },

  // Tạo bài đăng mới với attachments
  create: async (
  data,
  images,
  video,
  token) =>
  {
    const formData = new FormData();
    formData.append("Content", data.content);
    if (data.userId) formData.append("UserId", data.userId.toString());
    if (data.companyId) formData.append("CompanyId", data.companyId.toString());

    if (images && images.length > 0) {
      images.forEach((image) => {
        formData.append("Images", image);
      });
    }

    if (video) {
      formData.append("Video", video);
    }

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}`,
      {
        method: "POST",
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: formData
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || "Failed to create post");
    }

    return response.json();
  },

  // Cập nhật bài đăng (chỉ text)
  update: (id, data, token) => {
    return apiPut(`${ENDPOINT}/${id}`, data, {
      token
    });
  },

  // Cập nhật bài đăng với ảnh
  updateWithImages: async (
  id,
  data,
  images,
  token) =>
  {
    const formData = new FormData();
    formData.append("Content", data.content);
    if (data.userId) formData.append("UserId", data.userId.toString());
    if (data.companyId) formData.append("CompanyId", data.companyId.toString());

    if (images && images.length > 0) {
      images.forEach((image) => {
        formData.append("Images", image);
      });
    }

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${id}`,
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
      throw new Error(error.message || "Failed to update post");
    }

    return response.json();
  },

  // Xóa bài đăng
  delete: (id, token) => {
    return apiDelete(`${ENDPOINT}/${id}`, { token });
  },

  // Lấy comments của bài đăng
  getComments: (
  postId,
  pageNumber = 1,
  pageSize = 10,
  token) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${postId}/comments`,
      pageNumber,
      pageSize,
      { token }
    );
  },

  // Toggle like bài đăng
  toggleLike: (postId, userId, token) => {
    return apiPost(
      `${ENDPOINT}/${postId}/like`,
      { userId },
      { token }
    );
  },

  // Thêm comment
  addComment: async (
  postId,
  content,
  userId,
  attachments,
  token) =>
  {
    const formData = new FormData();
    formData.append("Content", content);
    formData.append("UserId", userId.toString());
    formData.append("PostId", postId.toString());

    if (attachments && attachments.length > 0) {
      attachments.forEach((file) => {
        formData.append("Attachments", file);
      });
    }

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${postId}/comment`,
      {
        method: "POST",
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: formData
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || "Failed to add comment");
    }

    return response.json();
  },

  // Xóa comment
  deleteComment: (
  postId,
  commentId,
  userId,
  token) =>
  {
    return apiDelete(`${ENDPOINT}/${postId}/comment/${commentId}`, {
      params: { userId },
      token
    });
  }
};