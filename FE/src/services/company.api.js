import { apiGet, apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById, apiUploadFile } from './api';

import { getUserRole } from '@/utils/auth';

const ENDPOINT = '/api/Companies';

export const companyApi = {
  // Lấy danh sách công ty với phân trang
  getAll: (pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, { token });
  },

  // Lấy chi tiết công ty theo ID
  getById: (id, token) => {
    return apiGetById(ENDPOINT, id, { token });
  },

  // Tạo công ty mới
  create: (data, token) => {
    return apiPost(ENDPOINT, data, { token });
  },

  // Cập nhật công ty
  update: (id, data, token) => {
    return apiPut(`${ENDPOINT}/${id}`, data, { token });
  },

  // Xóa công ty
  delete: (id, token) => {
    return apiDelete(`${ENDPOINT}/${id}`, { token });
  },

  // Tìm kiếm công ty
  search: (keyword, pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { keyword },
      token
    });
  },

  // Lấy danh sách logo công ty
  getLogos: (pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(`${ENDPOINT}/logos`, pageNumber, pageSize, { token });
  },

  // ===== HR Company Management =====

  // Lấy công ty của HR đang đăng nhập
  getMyCompany: (token) => {
    const role = getUserRole();
    return apiGet(`${ENDPOINT}/my-company`, { token, params: { role } });
  },

  // Upload ảnh đại diện công ty
  uploadAvatar: (file, token) => {
    return apiUploadFile(`${ENDPOINT}/upload-avatar`, file, 'file', { token });
  },

  // Upload ảnh bìa công ty
  uploadCover: (file, token) => {
    return apiUploadFile(`${ENDPOINT}/upload-cover`, file, 'file', { token });
  },

  // Cập nhật thông tin công ty
  updateMyCompany: (data, token) => {
    return apiPut(`${ENDPOINT}/my-company`, data, { token });
  }
};