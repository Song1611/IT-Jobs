import { apiGet, apiPost, apiPut, apiDelete } from './api';

const ENDPOINT = '/api/skills';

export const skillApi = {
  getAll: () => {
    return apiGet(ENDPOINT);
  },

  getById: (id) => {
    return apiGet(`${ENDPOINT}/${id}`);
  },

  create: (data) => {
    return apiPost(ENDPOINT, data);
  },

  update: (id, data) => {
    return apiPut(`${ENDPOINT}/${id}`, data);
  },

  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  }
};
