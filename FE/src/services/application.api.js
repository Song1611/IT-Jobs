import { apiPost, apiGet, apiGetPaginated, apiPut, apiDelete } from './api';

import { getUserRole } from '@/utils/auth';

const ENDPOINT = '/api/Application';







export const applicationApi = {
  // Tạo đơn ứng tuyển mới
  create: (data, token) => {
    return apiPost(ENDPOINT, data, { token });
  },

  // Lấy danh sách đơn ứng tuyển của user
  getByUser: (userId, pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(`${ENDPOINT}/user/${userId}`, pageNumber, pageSize, { token });
  },

  // Lấy danh sách đơn ứng tuyển theo job
  getByJob: (jobId, pageNumber = 1, pageSize = 10, token) => {
    return apiGetPaginated(`${ENDPOINT}/job/${jobId}`, pageNumber, pageSize, { token });
  },

  // Lấy danh sách đơn ứng tuyển theo company
  getByCompany: (companyId, pageNumber = 1, pageSize = 10, token) => {
    const role = getUserRole();
    return apiGetPaginated(`${ENDPOINT}/company/${companyId}`, pageNumber, pageSize, {
      token,
      params: { role }
    });
  },

  // Lấy chi tiết đơn ứng tuyển
  getById: (id, token) => {
    return apiGet(`${ENDPOINT}/${id}`, { token });
  },

  // Cập nhật trạng thái đơn ứng tuyển (chấp nhận/từ chối)
  updateStatus: (jobId, userId, data, token) => {
    return apiPut(`${ENDPOINT}/${jobId}/${userId}`, data, { token });
  },

  // Chấp nhận đơn ứng tuyển
  accept: (jobId, userId, cvUrl, coverLetter, token) => {
    return apiPut(
      `${ENDPOINT}/${jobId}/${userId}`,
      { cvUrl, coverLetter, status: 'accepted' },
      { token }
    );
  },

  // Từ chối đơn ứng tuyển
  reject: (jobId, userId, cvUrl, coverLetter, token) => {
    return apiPut(
      `${ENDPOINT}/${jobId}/${userId}`,
      { cvUrl, coverLetter, status: 'rejected' },
      { token }
    );
  },

  // Xóa đơn ứng tuyển
  delete: (jobId, userId, token) => {
    return apiDelete(`${ENDPOINT}/${jobId}/${userId}`, { token });
  }
};