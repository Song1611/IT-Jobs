package com.itjob.service.impl;

import com.itjob.redis.CacheName;
import com.itjob.dto.response.CompanyResponse;
import com.itjob.entity.Company;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.CompanyMapper;
import com.itjob.repository.CompanyRepository;
import com.itjob.service.CompanyCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyCacheServiceImpl implements CompanyCacheService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    @Cacheable(value = CacheName.COMPANY_BY_ID,
               key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    public CompanyResponse getCachedCompanyById(UUID id) {
        log.debug("Fetching company {} from database", id);
        Company company = companyRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        return companyMapper.toCompanyResponse(company);
    }

    @Override
    @Cacheable(value = CacheName.COMPANY_BY_SLUG,
               key = "T(com.itjob.util.CacheKeyGenerator).forSlug(#slug)")
    public CompanyResponse getCachedCompanyBySlug(String slug) {
        log.debug("Fetching company by slug {} from database", slug);
        Company company = companyRepository.findBySlugAndIsDeleted(slug, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        return companyMapper.toCompanyResponse(company);
    }
}
