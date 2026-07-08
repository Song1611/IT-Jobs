import { apiGet, apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById } from './api';

import { getUserRole } from '@/utils/auth';

const ENDPOINT = '/api/jobs';
const HR_ENDPOINT = '/api/hr';

export const jobApi = {
  // Lấy danh sách công việc
  getAll: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  // Lấy chi tiết công việc
  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  // Lấy công việc theo công ty
  getByCompany: (companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${HR_ENDPOINT}/companies/${companyId}/jobs`, pageNumber, pageSize);
  },

  // Tạo công việc mới
  create: (companyId, data) => {
    return apiPost(`${HR_ENDPOINT}/companies/${companyId}/jobs`, data);
  },

  // Cập nhật công việc
  update: (id, data) => {
    // Nếu có companyId trong data, có thể dùng: `${HR_ENDPOINT}/companies/${data.companyId}/jobs/${id}`
    // Tạm thời gọi theo update job chung nếu có
    return apiPut(`${HR_ENDPOINT}/companies/${data.companyId || 0}/jobs/${id}`, data);
  },

  // Xóa công việc
  delete: (id, companyId = 0) => {
    return apiDelete(`${HR_ENDPOINT}/companies/${companyId}/jobs/${id}`);
  },

  // Tìm kiếm công việc (Dùng Specification Pattern mới)
  search: (keyword, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { filter: `title~${keyword}` }
    });
  },

  // Lấy công việc nổi bật (thay cho today)
  getFeatured: (limit = 10) => {
    return apiGet(`${ENDPOINT}/featured`, { params: { limit } });
  },

  // Lấy công việc theo skill
  getBySkill: (skillId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { filter: `skills.id:${skillId}` }
    });
  },

  // Lấy công việc theo user (HR)
  getByUser: (userId, pageNumber = 1, pageSize = 10) => {
    // Giả định userId = companyId cho HR
    return apiGetPaginated(`${HR_ENDPOINT}/companies/${userId}/jobs`, pageNumber, pageSize);
  }
};