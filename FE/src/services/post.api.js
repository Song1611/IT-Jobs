import {
  apiPost,
  apiPut,
  apiDelete,
  apiGetPaginated,
  apiGetById,
  apiGet } from
"./api";

const ENDPOINT = "/api/posts";

function getToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export const postApi = {
  // Lấy danh sách bài đăng với pagination
  getAll: (
  pageNumber = 1,
  pageSize = 10,
  currentUserId) =>
  {
    const params = {};
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, { params });
  },

  // Lấy chi tiết bài đăng
  getById: (id, currentUserId) => {
    const params = {};
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGet(`${ENDPOINT}/${id}`, { params });
  },

  // Lấy bài đăng theo user
  getByUser: (
  userId,
  pageNumber = 1,
  pageSize = 10,
  currentUserId) =>
  {
    const params = {};
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGetPaginated(`${ENDPOINT}/user/${userId}`, pageNumber, pageSize, { params });
  },

  // Lấy bài đăng theo company
  getByCompany: (
  companyId,
  pageNumber = 1,
  pageSize = 10,
  currentUserId) =>
  {
    const params = {};
    if (currentUserId) params.currentUserId = currentUserId;
    return apiGetPaginated(`${ENDPOINT}/company/${companyId}`, pageNumber, pageSize, { params });
  },

  // Tạo bài đăng mới với attachments
  create: async (
  data,
  images,
  video) =>
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
    
    const token = getToken();

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

    const json = await response.json();
    return json.code !== undefined && json.code !== 1000
      ? Promise.reject(new Error(json.message || `API Error: ${json.code}`))
      : (json.result !== undefined ? json.result : json);
  },

  // Cập nhật bài đăng (chỉ text)
  update: (id, data) => {
    return apiPut(`${ENDPOINT}/${id}`, data);
  },

  // Cập nhật bài đăng với ảnh
  updateWithImages: async (
  id,
  data,
  images) =>
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
    
    const token = getToken();

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

    const json = await response.json();
    return json.code !== undefined && json.code !== 1000
      ? Promise.reject(new Error(json.message || `API Error: ${json.code}`))
      : (json.result !== undefined ? json.result : json);
  },

  // Xóa bài đăng
  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  },

  // Lấy comments của bài đăng
  getComments: (
  postId,
  pageNumber = 1,
  pageSize = 10) =>
  {
    return apiGetPaginated(
      `${ENDPOINT}/${postId}/comments`,
      pageNumber,
      pageSize
    );
  },

  // Toggle like bài đăng
  toggleLike: (postId, userId) => {
    return apiPost(
      `/api/reactions/posts/${postId}`,
      {},
      { params: { type: 'LIKE' } }
    );
  },

  // Thêm comment
  addComment: async (
  postId,
  content,
  userId,
  attachments) =>
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
    
    const token = getToken();

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

    const json = await response.json();
    if (json && typeof json.code === "number" && json.code !== 1000) {
      throw new Error(json.message || "Failed to add comment");
    }
    return json.result !== undefined ? json.result : json;
  },

  // Xóa comment
  deleteComment: (
  postId,
  commentId,
  userId) =>
  {
    return apiDelete(`${ENDPOINT}/${postId}/comment/${commentId}`, {
      params: { userId }
    });
  }
};
