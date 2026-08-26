import axios from 'axios';

const BE_ENDPOINT = process.env.NEXT_PUBLIC_BE_ENDPOINT;

// Helper function để lấy role từ localStorage
function getUserRole() {
  if (typeof window === 'undefined') return '';
  const userInfo = localStorage.getItem('userInfo');
  if (!userInfo) return '';
  try {
    const parsed = JSON.parse(userInfo);
    return parsed?.role || '';
  } catch {
    return '';
  }
}

let inMemoryToken = null;

export function setAuthToken(token) {
  inMemoryToken = token;
}

// Helper function để lấy access token từ memory
function getAuthToken() {
  return inMemoryToken;
}

// Tạo axios instance
const api = axios.create({
  baseURL: BE_ENDPOINT,
  headers: {
    "Content-Type": "application/json"
  },
  withCredentials: true, // Support HttpOnly cookie
  paramsSerializer: {
    indexes: null
  }
});

// A separate instance for refreshing token to avoid interceptor loops
const apiRefresh = axios.create({
  baseURL: BE_ENDPOINT,
  headers: {
    "Content-Type": "application/json"
  },
  withCredentials: true
});

let isRefreshing = false;
let refreshSubscribers = [];

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb);
}

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
}

// Interceptor request
api.interceptors.request.use(
  (config) => {
    const token = config.token || getAuthToken();
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }

    const role = getUserRole();
    if (role) {
      config.params = { ...config.params, role };
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor response
api.interceptors.response.use(
  (response) => {
    const data = response.data;

    // Handle Backend API custom errors (code !== 1000)
    if (data && typeof data.code === "number") {
      if (data.code !== 1000) {
        throw new Error(data.message || `API Error: ${data.code}`);
      }
      return data.result !== undefined ? data.result : data;
    }

    return data;
  },
  async (error) => {
    const originalRequest = error.config;
    
    // Check if error is 401 and we haven't retried yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      const isAuthEndpoint = originalRequest.url?.includes('/api/auth/refresh') ||
                             originalRequest.url?.includes('/api/auth/login') ||
                             originalRequest.url?.includes('/api/auth/register');
                             
      // Only attempt to refresh token if it's NOT an auth endpoint
      if (!isAuthEndpoint) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            subscribeTokenRefresh((token, err) => {
              if (err) {
                reject(err);
              } else {
                originalRequest.headers['Authorization'] = `Bearer ${token}`;
                resolve(api(originalRequest));
              }
            });
          });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
          // Backend now reads refresh token from HttpOnly cookie
          const res = await apiRefresh.post('/api/auth/refresh');
          
          // Ensure successful refresh returns data
          if (res.data?.code === 1000 || res.data?.result) {
              const resultData = res.data.result || res.data;
              const newAccessToken = resultData.accessToken;
              
              setAuthToken(newAccessToken);
              
              // Notify React state (AuthProvider) that token has been refreshed
              if (typeof window !== 'undefined') {
                window.dispatchEvent(new CustomEvent('token_refreshed', { detail: newAccessToken }));
              }
              
              isRefreshing = false;
              onRefreshed(newAccessToken);
              
              originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
              return api(originalRequest);
          } else {
              throw new Error("Refresh failed");
          }
        } catch (refreshError) {
          isRefreshing = false;
          
          // Create a standard error
          const finalError = new Error("Session expired or silent refresh failed");
          
          // Reject all queued requests
          refreshSubscribers.forEach(cb => cb(null, finalError));
          refreshSubscribers = [];
          
          // Logout logic
          setAuthToken(null);
          if (typeof window !== "undefined") {
            localStorage.removeItem("userInfo");
            localStorage.removeItem("company");
            if (window.location.pathname !== "/login" && window.location.pathname !== "/register") {
              window.location.href = "/login";
            }
          }
          
          return Promise.reject(finalError);
        }
      }
    }

    const data = error.response?.data;
    
    // Handle Backend API custom errors (code !== 1000) for non-2xx status codes
    if (data && typeof data.code === "number" && data.code !== 1000) {
      return Promise.reject(new Error(data.message || `API Error: ${data.code}`));
    }
    
    const message = data?.message || data?.error || error.message || `HTTP Error: ${error.response?.status}`;
    return Promise.reject(new Error(message));
  }
);

// GET request
export async function apiGet(endpoint, config) {
  try {
    const response = await api.get(endpoint, {
      ...config,
      token: config?.token,
      headers: { ...config?.headers }
    });
    return response;
  } catch (error) {
    throw new Error(
      `${error instanceof Error ? error.message : "Network error"}`
    );
  }
}

// POST request
export async function apiPost(endpoint, data, config) {
  const response = await api.post(endpoint, data, {
    ...config,
    token: config?.token,
    headers: { ...config?.headers }
  });
  return response;
}

// POST request with FormData (for file uploads)
export async function apiUploadFile(endpoint, file, fieldName = 'file', config) {
  const formData = new FormData();
  formData.append(fieldName, file);

  const response = await api.post(endpoint, formData, {
    ...config,
    token: config?.token,
    headers: {
      ...config?.headers
    }
  });
  return response;
}

// PUT request
export async function apiPut(endpoint, data, config) {
  const response = await api.put(endpoint, data, {
    ...config,
    token: config?.token,
    headers: { ...config?.headers }
  });
  return response;
}

// DELETE request
export async function apiDelete(endpoint, config) {
  const response = await api.delete(endpoint, {
    ...config,
    token: config?.token,
    headers: { ...config?.headers }
  });
  return response;
}

// PATCH request
export async function apiPatch(endpoint, data, config) {
  const response = await api.patch(endpoint, data, {
    ...config,
    token: config?.token,
    headers: { ...config?.headers }
  });
  return response;
}

// Helper: GET với phân trang
export async function apiGetPaginated(
  endpoint,
  pageNumber = 1,
  pageSize = 10,
  config
) {
  return apiGet(endpoint, {
    ...config,
    params: {
      page: Math.max(0, pageNumber - 1),
      size: pageSize,
      ...config?.params
    }
  });
}

// Helper: GET by ID
export async function apiGetById(endpoint, id, config) {
  return apiGet(`${endpoint}/${id}`, config);
}