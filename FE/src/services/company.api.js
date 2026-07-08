import { apiGet, apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById, apiUploadFile } from './api';

import { getUserRole } from '@/utils/auth';

const ENDPOINT = '/api/companies';
const HR_ENDPOINT = '/api/hr';

export const companyApi = {
  // Lấy danh sách công ty với phân trang
  getAll: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  // Lấy chi tiết công ty theo ID
  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  // Tạo công ty mới
  create: (data) => {
    return apiPost(`${HR_ENDPOINT}/companies`, data);
  },

  // Cập nhật công ty
  update: (id, data) => {
    return apiPut(`${HR_ENDPOINT}/companies/${id}`, data);
  },

  // Xóa công ty
  delete: (id) => {
    return apiDelete(`${HR_ENDPOINT}/companies/${id}`);
  },

  // Tìm kiếm công ty
  search: (keyword, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { filter: `name~${keyword}` }
    });
  },

  // Lấy danh sách logo công ty (tạm dùng top hoặc getAll)
  getLogos: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/top`, pageNumber, pageSize);
  },

  // ===== HR Company Management =====

  // Lấy công ty của HR đang đăng nhập
  getMyCompany: () => {
    return apiGet(`${HR_ENDPOINT}/my-company`);
  },

  // Upload ảnh đại diện công ty
  uploadAvatar: (file) => {
    return apiUploadFile(`${HR_ENDPOINT}/my-company/avatar`, file, 'file');
  },

  // Upload ảnh bìa công ty
  uploadCover: (file) => {
    return apiUploadFile(`${HR_ENDPOINT}/my-company/cover`, file, 'file');
  },

  // Cập nhật thông tin công ty
  updateMyCompany: (data) => {
    return apiPut(`${HR_ENDPOINT}/my-company`, data);
  }
};