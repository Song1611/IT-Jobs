import { apiGet } from './api';

const ENDPOINT = '/api/Search';

















































export const searchApi = {
  search: (keyword, pageNumber = 1, pageSize = 10) => {
    return apiGet(ENDPOINT, {
      params: {
        Keyword: keyword,
        PageNumber: pageNumber,
        PageSize: pageSize
      }
    });
  }
};