package com.itjob.constant;

import java.util.List;

public final class SecurityConstant {

    private SecurityConstant() {
    }

    public static final List<String> ALLOWED_CORS_METHODS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/api/companies/**",
            "/api/jobs/**",
            "/api/blogs/**"
    };
}
