// Utility functions để làm việc với user authentication










/**
 * Lấy thông tin user từ localStorage
 */
export const getUserInfo = () => {
  if (typeof window === 'undefined') return null;

  const userInfo = localStorage.getItem('userInfo');
  if (!userInfo) return null;

  try {
    return JSON.parse(userInfo);
  } catch {
    return null;
  }
};

/**
 * Lấy role của user hiện tại
 */
export const getUserRole = () => {
  const userInfo = getUserInfo();
  return userInfo?.role || '';
};

/**
 * Lấy ID của user hiện tại
 */
export const getUserId = () => {
  const userInfo = getUserInfo();
  return userInfo?.id || null;
};

/**
 * Kiểm tra user có phải là admin không
 */
export const isAdmin = () => {
  return getUserRole() === 'admin';
};

/**
 * Kiểm tra user có phải là employer không
 */
export const isEmployer = () => {
  return getUserRole() === 'employer';
};

/**
 * Kiểm tra user có phải là user thường không
 */
export const isUser = () => {
  return getUserRole() === 'user';
};

/**
 * Kiểm tra user đã đăng nhập chưa
 */
export const isAuthenticated = () => {
  return getUserInfo() !== null;
};

/**
 * Lưu thông tin user vào localStorage
 */
export const setUserInfo = (userInfo) => {
  if (typeof window === 'undefined') return;
  localStorage.setItem('userInfo', JSON.stringify(userInfo));
};

/**
 * Xóa thông tin user khỏi localStorage
 */
export const clearUserInfo = () => {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('userInfo');
};