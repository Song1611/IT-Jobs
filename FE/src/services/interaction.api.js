import { apiPost, apiDelete, apiGetPaginated } from "./api";


const ENDPOINT = "/api/Post";

export const interactionApi = {
  // Toggle like cho bài post
  toggleLike: (postId, userId, token) => {
    return apiPost(
      `${ENDPOINT}/${postId}/like`,
      { postId, userId },
      { token }
    );
  },

  // Lấy comments của bài post với pagination
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

  // Thêm comment vào bài post
  addComment: async (
  postId,
  userId,
  content,
  token,
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

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_BE_ENDPOINT}${ENDPOINT}/${postId}/comment`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`
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
      token,
      params: { userId }
    });
  }
};