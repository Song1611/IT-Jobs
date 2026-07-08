import {
  apiGet,
  apiPost,
  apiPut,
  apiDelete,
  apiGetPaginated,
  apiGetById } from
"./api";

import { getUserRole } from '@/utils/auth';

const ENDPOINT = "/api/Blog";

function getToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export const blogApi = {
  // Lấy danh sách blog
  getAll: (pageNumber = 1, pageSize = 10, categoryId) => {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: categoryId ? { categoryId } : undefined
    });
  },

  // Lấy chi tiết blog
  getById: (id) => {
    return apiGetById(ENDPOINT, id);
  },

  // Lấy blog theo userId
  getByUserId: (userId) => {
    const role = getUserRole();
    return apiGet(`${ENDPOINT}/user/${userId}`, { params: { role } });
  },

  // Tạo blog mới với multipart/form-data
  create: async (formData) => {
    const BE_ENDPOINT = process.env.NEXT_PUBLIC_BE_ENDPOINT;

    console.log("Creating blog with FormData:");
    for (let pair of formData.entries()) {
      console.log(pair[0], pair[1]);
    }
    
    const token = getToken();

    const response = await fetch(`${BE_ENDPOINT}${ENDPOINT}`, {
      method: "POST",
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
        // Không set Content-Type, để browser tự động set với boundary
      },
      body: formData
    });

    console.log("Response status:", response.status);
    console.log("Response headers:", response.headers);

    if (!response.ok) {
      const error = await response.json().catch(() => ({
        message: response.statusText
      }));
      console.error("Error response:", error);
      throw new Error(error.message || `HTTP Error: ${response.status}`);
    }

    return response.json();
  },

  // Cập nhật blog với multipart/form-data
  update: async (id, formData) => {
    const BE_ENDPOINT = process.env.NEXT_PUBLIC_BE_ENDPOINT;
    
    const token = getToken();
    
    const response = await fetch(`${BE_ENDPOINT}${ENDPOINT}/${id}`, {
      method: "PUT",
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: formData
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({
        message: response.statusText
      }));
      throw new Error(error.message || `HTTP Error: ${response.status}`);
    }

    return response.json();
  },

  // Xóa blog
  delete: (id) => {
    return apiDelete(`${ENDPOINT}/${id}`);
  },

  // Tìm kiếm blog
  search: (
  keyword,
  pageNumber = 1,
  pageSize = 10) =>
  {
    return apiGetPaginated(ENDPOINT, pageNumber, pageSize, {
      params: { keyword }
    });
  },

  // Lấy danh sách categories
  getCategories: () => {
    return apiGet("/api/BlogCategory");
  }
};