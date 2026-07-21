package com.itjob.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum CacheTTL {

    // ========== JOB CACHES ==========
    JOB_DETAIL(CacheName.JOB_DETAIL, Duration.ofMinutes(10)),
    JOB_SEARCH(CacheName.JOB_SEARCH, Duration.ofMinutes(5)),
    JOB_FEATURED(CacheName.JOB_FEATURED, Duration.ofMinutes(15)),
    JOB_BY_COMPANY(CacheName.JOB_BY_COMPANY, Duration.ofMinutes(10)),

    // ========== COMPANY CACHES ==========
    COMPANY_BY_ID(CacheName.COMPANY_BY_ID, Duration.ofMinutes(30)),
    COMPANY_BY_SLUG(CacheName.COMPANY_BY_SLUG, Duration.ofMinutes(30)),
    COMPANY_MY(CacheName.COMPANY_MY, Duration.ofSeconds(30)),
    COMPANY_SEARCH(CacheName.COMPANY_SEARCH, Duration.ofMinutes(10)),
    COMPANY_FEATURED(CacheName.COMPANY_FEATURED, Duration.ofMinutes(30)),

    // ========== BLOG CACHES ==========
    BLOG_DETAIL(CacheName.BLOG_DETAIL, Duration.ofMinutes(15)),
    BLOG_SEARCH(CacheName.BLOG_SEARCH, Duration.ofMinutes(5)),
    BLOG_RECENT(CacheName.BLOG_RECENT, Duration.ofMinutes(10)),
    BLOG_BY_CATEGORY(CacheName.BLOG_BY_CATEGORY, Duration.ofMinutes(10)),

    // ========== REFERENCE DATA CACHES (Stable) ==========
    SKILL_LIST(CacheName.SKILL_LIST, Duration.ofHours(12)),
    SKILL_DETAIL(CacheName.SKILL_DETAIL, Duration.ofHours(12)),

    LOCATION_LIST(CacheName.LOCATION_LIST, Duration.ofDays(1)),
    LOCATION_DETAIL(CacheName.LOCATION_DETAIL, Duration.ofDays(1)),

    BLOG_CATEGORY_LIST(CacheName.BLOG_CATEGORY_LIST, Duration.ofHours(6)),
    BLOG_CATEGORY_DETAIL(CacheName.BLOG_CATEGORY_DETAIL, Duration.ofHours(6)),

    // ========== REVIEW CACHES ==========
    REVIEW_DETAIL(CacheName.REVIEW_DETAIL, Duration.ofMinutes(30)),
    REVIEW_BY_COMPANY(CacheName.REVIEW_BY_COMPANY, Duration.ofMinutes(30)),

    // ========== DASHBOARD CACHES (Short TTL) ==========
    DASHBOARD_ADMIN(CacheName.DASHBOARD_ADMIN, Duration.ofMinutes(5)),
    DASHBOARD_HR(CacheName.DASHBOARD_HR, Duration.ofMinutes(5)),

    // ========== VIEW COUNTER TTL (raw Redis keys, not Spring Cache) ==========
    VIEW_KEY("view_key", Duration.ofDays(7)),
    VIEW_DEBOUNCE("view_debounce", Duration.ofMinutes(5));

    private final String cacheName;
    private final Duration ttl;

    public boolean isSpringCache() {
        return !cacheName.startsWith("view_");
    }


}
