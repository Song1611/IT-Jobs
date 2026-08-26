"use client";
import { jwtDecode } from "jwt-decode";
import React, { createContext, useContext, useState, useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { authApi } from "@/services/auth.api";
import { companyApi } from "@/services/company.api";
import { setAuthToken } from "@/services/api";






import Routes from "@/constants/routes";

// JWT Payload interface based on your token structure


































const AuthContext = createContext(undefined);

// Function to check if user has access to a specific route based on role
const hasRouteAccess = (role, pathname) => {
  if (!role) return false;

  const normalizedRole = role.toLowerCase().trim();

  // Admin can ONLY access /admin/*
  if (normalizedRole === "admin") {
    if (pathname.startsWith("/admin")) {
      return true;
    }
    // Admin cannot access /hr or other routes
    if (pathname.startsWith("/hr")) {
      return false;
    }
    return true; // Can access public routes
  }

  // Employer/HR can ONLY access /hr/* (NOT /admin/*)
  if (normalizedRole === "employer" || normalizedRole === "hr") {
    if (pathname.startsWith("/admin")) {
      return false;
    }
    if (pathname.startsWith("/hr")) {
      return true;
    }
    return true; // Can access public routes
  }

  // User can NOT access /hr/* and /admin/*
  if (normalizedRole === "user") {
    if (pathname.startsWith("/hr") || pathname.startsWith("/admin")) {
      return false;
    }
    return true;
  }

  return false;
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [company, setCompany] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isInitializing, setIsInitializing] = useState(true);
  const [jwtPayload, setJwtPayload] = useState(null);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    const initializeAuth = async () => {
      if (typeof window === "undefined") return;
      
      const storedUser = localStorage.getItem("userInfo");
      const storedCompany = localStorage.getItem("company");

      // Set initial user info if available
      if (storedUser) {
        setUser(JSON.parse(storedUser));
      }
      if (storedCompany) {
        setCompany(JSON.parse(storedCompany));
      }

      // Try silent refresh to get token
      try {
        const response = await authApi.refreshToken();
        if (response.success && response.data) {
          const { accessToken, user: userData } = response.data;
          
          setAuthToken(accessToken);
          setToken(accessToken);
          
          // Re-sync user info just in case
          if (userData) {
            setUser(userData.result || userData);
            localStorage.setItem("userInfo", JSON.stringify(userData.result || userData));
          }

          try {
            const decoded = jwtDecode(accessToken);
            setJwtPayload(decoded);
            
            // If user is HR/Employer but company is missing, fetch it
            if ((decoded.role === 'employer' || decoded.role === 'hr') && !storedCompany) {
               const companyData = await companyApi.getMyCompany(accessToken);
               if (companyData) {
                 setCompany(companyData);
                 localStorage.setItem("company", JSON.stringify(companyData));
               }
            }
          } catch (error) {
            console.error("Failed to decode JWT token:", error);
          }
        }
      } catch (error) {
        // Silent refresh failed (e.g. no cookie or expired cookie)
        console.log("Silent refresh failed or no session");
        // Clear old storage just in case
        localStorage.removeItem("userInfo");
        localStorage.removeItem("company");
        setUser(null);
        setCompany(null);
      } finally {
        setIsInitializing(false);
        setLoading(false);
      }
    };

    initializeAuth();

    // Listen to token_refreshed event from api.js interceptor
    const handleTokenRefreshed = (e) => {
      const newToken = e.detail;
      setToken(newToken);
      setAuthToken(newToken);
      try {
        setJwtPayload(jwtDecode(newToken));
      } catch (error) {
        console.error("Failed to decode refreshed JWT token:", error);
      }
    };

    if (typeof window !== "undefined") {
      window.addEventListener("token_refreshed", handleTokenRefreshed);
      return () => {
        window.removeEventListener("token_refreshed", handleTokenRefreshed);
      };
    }
  }, []);

  // Route protection effect
  useEffect(() => {
    if (isInitializing || loading || !pathname) return;

    // Don't check access-denied page itself
    if (pathname === "/access-denied") return;

    // Only check protected routes
    if (pathname.startsWith("/hr") || pathname.startsWith("/admin")) {
      const role = jwtPayload?.role || user?.role;

      if (!token || !role) {
        // Not logged in, redirect to login
        router.push("/login");
        return;
      }

      if (!hasRouteAccess(role, pathname)) {
        // No access, redirect to access denied page
        router.push("/access-denied");
      }
    }
  }, [pathname, loading, isInitializing, jwtPayload, user, token, router]);

  // Check if current user has access to a specific route
  const checkRouteAccess = (routePath) => {
    const role = jwtPayload?.role || user?.role;
    return hasRouteAccess(role, routePath);
  };

  // Helper function to get redirect path based on role
  const getRedirectPath = (role) => {
    switch (role) {
      case "admin":
        return "/admin";
      case "hr":
      case "employer":
        return "/hr";
      case "user":
      default:
        return Routes.home;
    }
  };

  const login = async (
  email,
  password) =>
  {
    try {
      const response = await authApi.login({ email, password });

      if (response.success && response.data) {
        const { accessToken, user: userData } = response.data;

        // Save to localStorage
        if (typeof window !== "undefined") {
          localStorage.setItem("userInfo", JSON.stringify(userData));
        }

        setUser(userData);
        setToken(accessToken);
        setAuthToken(accessToken);

        // If user is HR/Employer, fetch company info
        if (userData.role === 'employer' || userData.role === 'hr') {
          try {
            const companyData = await companyApi.getMyCompany(accessToken);
            if (companyData) {
              setCompany(companyData);
              if (typeof window !== "undefined") {
                localStorage.setItem("company", JSON.stringify(companyData));
              }
            }
          } catch (error) {
            console.error("Failed to fetch company info after login:", error);
          }
        }

        // Decode JWT token immediately after login
        try {
          const decoded = jwtDecode(accessToken);
          setJwtPayload(decoded);
        } catch (error) {
          console.error("Failed to decode JWT token on login:", error);
        }

        return { success: true, role: userData.role };
      }

      return {
        success: false,
        error: response.message || "Đăng nhập thất bại"
      };
    } catch (error) {
      console.error("Login error:", error);
      let errorMessage = error.message;
      if (errorMessage === "Unauthenticated" || errorMessage === "Unauthorized" || errorMessage.includes("401")) {
        errorMessage = "Email hoặc mật khẩu không đúng";
      }
      return {
        success: false,
        error: errorMessage || "Email hoặc mật khẩu không đúng"
      };
    }
  };

  const registerUser = async (
  data) =>
  {
    try {
      const response = await authApi.registerUser(data);
      return { success: true, data: response };
    } catch (error) {
      console.error("Register user error:", error);
      return {
        success: false,
        error: error.message || "Đăng ký thất bại. Vui lòng thử lại."
      };
    }
  };

  const registerHR = async (
  data) =>
  {
    try {
      const response = await authApi.registerHR(data);
      return { success: true, data: response };
    } catch (error) {
      console.error("Register HR error:", error);
      return {
        success: false,
        error: error.message || "Đăng ký thất bại. Vui lòng thử lại."
      };
    }
  };

  const logout = async () => {
    // Call backend to clear the httpOnly cookie
    try {
      await authApi.logout();
    } catch (error) {
      console.error("Logout API failed:", error);
    }
    
    // Remove from localStorage (only in browser)
    if (typeof window !== "undefined") {
      localStorage.removeItem("userInfo");
      localStorage.removeItem("company");
    }
    setUser(null);
    setCompany(null);
    setToken(null);
    setAuthToken(null);
    router.push("/");
  };

  // Update user data (for avatar, cover image, etc.)
  const updateUser = (userData) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updatedUser = { ...prev, ...userData };
      // Also update localStorage
      if (typeof window !== "undefined") {
        localStorage.setItem("userInfo", JSON.stringify(updatedUser));
      }
      return updatedUser;
    });
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        company,
        token,
        isAuthenticated: !!user && !!token,
        login,
        registerUser,
        registerHR,
        logout,
        updateUser,
        loading,
        isInitializing,
        getRedirectPath,
        checkRouteAccess
      }}>
      
      {children}
    </AuthContext.Provider>);

}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}