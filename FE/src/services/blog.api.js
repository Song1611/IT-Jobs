import {
  apiGet,
  apiPost,
  apiPut,
  apiDelete,
  apiGetPaginated,
  apiGetById } from
"./api";

const ENDPOINT = "/api/blogs";

export const blogApi = {
  getAll: (pageNumber = 1, pageSize = 10, categoryId, keyword) => {
    const params = { page: Math.max(0, pageNumber - 1), size: pageSize };
    const filters = [];
    if (keyword && keyword.trim()) {
      filters.push(`title~${keyword.trim()}`);
    }
    if (categoryId) {
      filters.push(`categoryId:${categoryId}`);
    }
    if (filters.length > 0) {
      params.filter = filters;
      return apiGet(ENDPOINT, { params });
    }
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize);
  },

  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  getRecent: (limit = 10) => {
    return apiGet(`${ENDPOINT}/recent`, { params: { limit } });
  },

  getMyBlogs: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/me`, pageNumber, pageSize);
  },

  getByUserId: (pageNumber = 1, pageSize = 10) => {
    return apiGetPaginated(`${ENDPOINT}/me`, pageNumber, pageSize);
  },

  create: (data) => {
    return apiPost(ENDPOINT, data);
  },

  update: (id, data) => {
    return apiPut(`${ENDPOINT}/${id}`, data);
  },

  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  },

  search: (keyword, pageNumber = 1, pageSize = 10, categoryId) => {
    const params = { page: Math.max(0, pageNumber - 1), size: pageSize };
    const filters = [`title~${keyword}`];
    if (categoryId) {
      filters.push(`categoryId:${categoryId}`);
    }
    params.filter = filters;
    return apiGet(ENDPOINT, { params });
  },

  getCategories: () => {
    return apiGet(`${ENDPOINT}/categories`);
  }
};
