import { apiPost, apiGet, apiGetPaginated, apiPut, apiDelete } from './api';

import { getUserRole } from '@/utils/auth';

const ENDPOINT = '/api/applications';
const HR_ENDPOINT = '/api/hr';

export const applicationApi = {
  // Tạo đơn ứng tuyển mới
  create: (data) => {
    return apiPost(ENDPOINT, data);
  },

  // Lấy danh sách đơn ứng tuyển của user
  getByUser: (userId, pageNumber = 1, pageSize = 10) => {
    // API mới dùng /api/v1/applications/me cho user hiện tại
    return apiGetPaginated(`${ENDPOINT}/me`, pageNumber, pageSize);
  },

  // Lấy danh sách đơn ứng tuyển theo job (HR)
  getByJob: (jobId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${HR_ENDPOINT}/jobs/${jobId}/applications`, pageNumber, pageSize);
  },

  // Lấy danh sách đơn ứng tuyển theo company (HR)
  getByCompany: (companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${HR_ENDPOINT}/companies/${companyId}/applications`, pageNumber, pageSize);
  },

  // Lấy chi tiết đơn ứng tuyển
  getById: (id) => {
    return apiGet(`${ENDPOINT}/${id}`);
  },

  // Cập nhật trạng thái đơn ứng tuyển (HR)
  // Lưu ý: API mới dùng id của application thay vì jobId + userId
  updateStatus: (id, status, notes = "") => {
    return apiPut(`${HR_ENDPOINT}/applications/${id}/status`, { status, notes });
  },

  // Chấp nhận đơn ứng tuyển
  accept: (id, notes) => {
    return applicationApi.updateStatus(id, 'accepted', notes);
  },

  // Từ chối đơn ứng tuyển
  reject: (id, notes) => {
    return applicationApi.updateStatus(id, 'rejected', notes);
  },

  // Rút đơn ứng tuyển (User)
  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  }
};