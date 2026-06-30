import { apiGet } from './api';






const ENDPOINT = '/api/Locations';

export const locationApi = {
  // Lấy danh sách tất cả tỉnh/thành phố
  getProvinces: () => {
    return apiGet(`${ENDPOINT}/provinces`);
  },

  // Lấy danh sách quận/huyện theo tỉnh/thành phố
  getWards: (provinceId) => {
    return apiGet(`${ENDPOINT}/wards`, {
      params: { provinceId }
    });
  }
};