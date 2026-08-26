import { apiGet, apiPost, apiPut, apiGetPaginated, apiGetById } from './api';

const ENDPOINT = '/api/companies';
const HR_ENDPOINT = '/api/hr';

export const companyApi = {
  getAll: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  getBySlug: (slug) => {
    return apiGet(`${ENDPOINT}/slug/${slug}`);
  },

  create: (data) => {
    return apiPost(ENDPOINT, data);
  },

  update: (id, data) => {
    return apiPut(`${ENDPOINT}/${id}`, data);
  },

  search: (keyword, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { filter: `name~${keyword}` }
    });
  },

  getLogos: (limit = 10) => {
    return apiGet(`${ENDPOINT}/top`, { params: { limit } });
  },

  getMyCompany: () => {
    return apiGet(`${ENDPOINT}/me`);
  },

  updateMyCompany: (data) => {
    return apiPut(`${ENDPOINT}/me`, data);
  }
};
