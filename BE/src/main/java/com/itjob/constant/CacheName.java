package com.itjob.constant;

public final class CacheName {

    private CacheName() {
    }

    // ========== JOB CACHES ==========
    public static final String JOB_DETAIL = "job_detail";
    public static final String JOB_SEARCH = "job_search";
    public static final String JOB_FEATURED = "job_featured";
    public static final String JOB_BY_COMPANY = "job_by_company";
    
    // ========== COMPANY CACHES ==========
    public static final String COMPANY_BY_ID = "company_by_id";
    public static final String COMPANY_BY_SLUG = "company_by_slug";
    public static final String COMPANY_SEARCH = "company_search";
    public static final String COMPANY_FEATURED = "company_featured";
    
    // ========== BLOG CACHES ==========
    public static final String BLOG_DETAIL = "blog_detail";
    public static final String BLOG_SEARCH = "blog_search";
    public static final String BLOG_RECENT = "blog_recent";
    public static final String BLOG_BY_CATEGORY = "blog_by_category";
    
    // ========== SKILL CACHES (Reference Data) ==========
    public static final String SKILL_LIST = "skill_list";
    public static final String SKILL_DETAIL = "skill_detail";
    
    // ========== LOCATION CACHES (Reference Data) ==========
    public static final String LOCATION_LIST = "location_list";
    public static final String LOCATION_DETAIL = "location_detail";
    
    // ========== BLOG CATEGORY CACHES (Reference Data) ==========
    public static final String BLOG_CATEGORY_LIST = "blog_category_list";
    public static final String BLOG_CATEGORY_DETAIL = "blog_category_detail";
    
    // ========== REVIEW CACHES ==========
    public static final String REVIEW_DETAIL = "review_detail";
    public static final String REVIEW_BY_COMPANY = "review_by_company";
    
    // ========== DASHBOARD CACHES (Short TTL) ==========
    public static final String DASHBOARD_ADMIN = "dashboard_admin";
    public static final String DASHBOARD_HR = "dashboard_hr";
}