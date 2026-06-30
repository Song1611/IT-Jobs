import { apiGet, apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById } from './api';


const ENDPOINT = '/api/skills';

export const skillApi = {
  // Lấy danh sách skills
  getAll: (pageNumber = 1, pageSize = 20, token) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, { token });
  },

  // Lấy chi tiết skill
  getById: (id, token) => {
    return apiGetById(ENDPOINT, id, { token });
  },

  // Tạo skill mới
  create: (data, token) => {
    return apiPost(ENDPOINT, data, { token });
  },

  // Cập nhật skill
  update: (id, data, token) => {
    return apiPut(`${ENDPOINT}/${id}`, data, { token });
  },

  // Xóa skill
  delete: (id, token) => {
    return apiDelete(`${ENDPOINT}/${id}`, { token });
  }
};