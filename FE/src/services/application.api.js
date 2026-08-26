import { apiPost, apiGet, apiGetPaginated, apiPatch, apiDelete } from './api';

const ENDPOINT = '/api/applications';
const HR_ENDPOINT = '/api/hr';

export const applicationApi = {
  create: (data) => {
    return apiPost(ENDPOINT, data);
  },

  getByUser: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/me`, pageNumber, pageSize);
  },

  getByJob: (jobId, companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${HR_ENDPOINT}/applications`, pageNumber, pageSize, {
      params: { jobId, companyId }
    });
  },

  getByCompany: (companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${HR_ENDPOINT}/applications/all`, pageNumber, pageSize, {
      params: { companyId }
    });
  },

  getById: (id) => {
    return apiGet(`${ENDPOINT}/${id}`);
  },

  updateStatus: (id, companyId, status, notes = "") => {
    return apiPatch(`${HR_ENDPOINT}/applications/${id}/status`, { status, notes }, {
      params: { companyId }
    });
  },

  markViewed: (id, companyId) => {
    return apiPatch(`${HR_ENDPOINT}/applications/${id}/viewed`, {}, {
      params: { companyId }
    });
  },

  accept: (id, companyId, notes) => {
    return applicationApi.updateStatus(id, companyId, 'approved', notes);
  },

  reject: (id, companyId, notes) => {
    return applicationApi.updateStatus(id, companyId, 'rejected', notes);
  },

  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  }
};
