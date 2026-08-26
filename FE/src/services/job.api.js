import { apiGet, apiPost, apiPut, apiDelete, apiGetPaginated, apiGetById } from './api';

const ENDPOINT = '/api/jobs';
const HR_ENDPOINT = '/api/hr';

export const jobApi = {
  getAll: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  getByCompany: (companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/company/${companyId}`, pageNumber, pageSize);
  },

  create: (companyId, data) => {
    return apiPost(`${ENDPOINT}/company/${companyId}`, data);
  },

  update: (id, companyId, data) => {
    return apiPut(`${ENDPOINT}/${id}/company/${companyId}`, data);
  },

  delete: (id, companyId) => {
    return apiDelete(`${ENDPOINT}/${id}/company/${companyId}`);
  },

  search: (keyword, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { keyword, filter: `title~${keyword}` }
    });
  },

  getFeatured: (limit = 10) => {
    return apiGet(`${ENDPOINT}/featured`, { params: { limit } });
  },

  getTrending: (limit = 20) => {
    return apiGet(`${ENDPOINT}/trending`, { params: { limit } });
  },

  getSuggestions: (q, limit = 10) => {
    return apiGet(`${ENDPOINT}/suggestions`, { params: { q, limit } });
  },

  getSearchHistory: (limit = 10) => {
    return apiGet(`${ENDPOINT}/search/history`, { params: { limit } });
  },

  getRecentlyViewed: (limit = 10) => {
    return apiGet(`${ENDPOINT}/recently-viewed`, { params: { limit } });
  },

  getRecommendations: (limit = 10) => {
    return apiGet(`${ENDPOINT}/recommendations`, { params: { limit } });
  },

  getBySkill: (skillId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { filter: `skills.id:${skillId}` }
    });
  },

  getByUser: (companyId, pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/company/${companyId}`, pageNumber, pageSize);
  }
};
