import { apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById } from './api';


const ENDPOINT = '/api/Reviews';

export const reviewApi = {
  // Lấy danh sách đánh giá
  getAll: (pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, { token });
  },

  // Lấy chi tiết đánh giá
  getById: (id, token) => {
    return apiGetById(ENDPOINT, id, { token });
  },

  // Lấy đánh giá theo công ty
  getByCompany: (companyId, pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(`${ENDPOINT}/company/${companyId}`, pageNumber, pageSize, { token });
  },

  // Lấy đánh giá theo user
  getByUser: (userId, pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(`${ENDPOINT}/user/${userId}`, pageNumber, pageSize, { token });
  },

  // Tạo đánh giá mới
  create: (data, token) => {
    return apiPost(ENDPOINT, data, { token });
  },

  // Cập nhật đánh giá
  update: (id, data, token) => {
    return apiPut(`${ENDPOINT}/${id}`, data, { token });
  },

  // Xóa đánh giá
  delete: (id, token) => {
    return apiDelete(`${ENDPOINT}/${id}`, { token });
  }
};