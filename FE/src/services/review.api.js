import { apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById } from './api';

const ENDPOINT = '/api/Reviews';

export const reviewApi = {
  // Lấy danh sách đánh giá
  getAll: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  // Lấy chi tiết đánh giá
  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  // Lấy đánh giá theo công ty
  getByCompany: (companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/company/${companyId}`, pageNumber, pageSize);
  },

  // Lấy đánh giá theo user
  getByUser: (userId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/user/${userId}`, pageNumber, pageSize);
  },

  // Tạo đánh giá mới
  create: (data) => {
    return apiPost(ENDPOINT, data);
  },

  // Cập nhật đánh giá
  update: (id, data) => {
    return apiPut(`${ENDPOINT}/${id}`, data);
  },

  // Xóa đánh giá
  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  }
};