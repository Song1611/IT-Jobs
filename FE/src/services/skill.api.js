import { apiGet, apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById } from './api';

const ENDPOINT = '/api/skills';

export const skillApi = {
  // Lấy danh sách skills
  getAll: (pageNumber = 1, pageSize = 20) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  // Lấy chi tiết skill
  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  // Tạo skill mới
  create: (data) => {
    return apiPost(ENDPOINT, data);
  },

  // Cập nhật skill
  update: (id, data) => {
    return apiPut(`${ENDPOINT}/${id}`, data);
  },

  // Xóa skill
  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  }
};