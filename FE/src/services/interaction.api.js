import { apiPost, apiDelete, apiGetPaginated } from "./api";

const ENDPOINT = "/api/posts";

function getToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export const interactionApi = {
  // Toggle like cho bài post
  toggleLike: (postId, userId) => {
    return apiPost(
      `/api/reactions/posts/${postId}`,
      {},
      { params: { type: 'LIKE' } }
    );
  },

  // Lấy comments của bài post với pagination
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

  // Thêm comment vào bài post
  addComment: async (
  postId,
  userId,
  content,
  attachments) =>
  {
    const formData = new FormData();
    formData.append("PostId", postId.toString());
    formData.append("UserId", userId.toString());
    formData.append("Content", content);

    if (attachments && attachments.length > 0) {
      attachments.forEach((file) => {
        formData.append("attachments", file);
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