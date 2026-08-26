import { apiPost, apiPut, apiDelete, apiGet, apiGetPaginated } from './api';

const ENDPOINT = '/api/reviews';
const COMPANY_ENDPOINT = '/api/companies';

export const reviewApi = {
  getById: (id) => {
    return apiGet(`${ENDPOINT}/${id}`);
  },

  getByCompany: (companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${COMPANY_ENDPOINT}/${companyId}/reviews`, pageNumber, pageSize);
  },

  getMyReviews: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/me`, pageNumber, pageSize);
  },

  create: (companyId, data) => {
    return apiPost(`${COMPANY_ENDPOINT}/${companyId}/reviews`, data);
  },

  update: (id, data) => {
    return apiPut(`${ENDPOINT}/${id}`, data);
  },

  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  }
};
