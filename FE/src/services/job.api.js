import { apiGet, apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById } from './api';

import { getUserRole } from '@/utils/auth';

const ENDPOINT = '/api/Job';

export const jobApi = {
  // Lấy danh sách công việc
  getAll: (pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, { token });
  },

  // Lấy chi tiết công việc
  getById: (id, token) => {
    return apiGetById(ENDPOINT, id, { token });
  },

  // Lấy công việc theo công ty
  getByCompany: (companyId, pageNumber = 1, pageSize = 10, token) => {
    const role = getUserRole();
    return apiGetPaginated(`${ENDPOINT}/by-company`, pageNumber, pageSize, {
      params: { companyId, role },
      token
    });
  },

  // Tạo công việc mới
  create: (companyId, data,






  token) => {
    return apiPost(`${ENDPOINT}/${companyId}`, data, { token });
  },

  // Cập nhật công việc
  update: (id, data, token) => {
    return apiPut(`${ENDPOINT}/${id}`, data, { token });
  },

  // Xóa công việc
  delete: (id, token) => {
    return apiDelete(`${ENDPOINT}/${id}`, { token });
  },

  // Tìm kiếm công việc
  search: (keyword, pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { keyword },
      token
    });
  },

  // Lấy công việc hôm nay
  getToday: (token) => {
    return apiGet(`${ENDPOINT}/today`, { token });
  },

  // Lấy công việc theo skill
  getBySkill: (skillId, pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(`${ENDPOINT}/by-skill`, pageNumber, pageSize, {
      params: { skillId },
      token
    });
  },

  // Lấy công việc theo user (HR)
  getByUser: (userId, pageNumber = 1, pageSize = 10, token) => {
    const role = getUserRole();
    return apiGetPaginated(`${ENDPOINT}/by-user/${userId}`, pageNumber, pageSize, { token, params: { role } });
  }
};