import { apiGet } from './api';

const ENDPOINT = '/api/provinces';

export const locationApi = {
  getProvinces: () => {
    return apiGet(ENDPOINT);
  }
};
