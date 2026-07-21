package com.itjob.service;

import com.itjob.dto.response.CompanyResponse;

import java.util.UUID;

public interface CompanyCacheService {

    CompanyResponse getCachedCompanyById(UUID id);

    CompanyResponse getCachedCompanyBySlug(String slug);
}
