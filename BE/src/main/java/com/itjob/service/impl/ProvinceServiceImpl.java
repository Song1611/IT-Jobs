package com.itjob.service.impl;

import com.itjob.redis.CacheName;
import com.itjob.dto.response.ProvinceResponse;
import com.itjob.entity.Province;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.ProvinceMapper;
import com.itjob.repository.ProvinceRepository;
import com.itjob.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceRepository provinceRepository;
    private final ProvinceMapper provinceMapper;

    @Override
    @Cacheable(value = CacheName.PROVINCE_LIST, key = "'list'")
    public List<ProvinceResponse> getAllProvinces() {
        log.info("Cache MISS - Fetching all provinces from database");
        return provinceRepository.findAll().stream()
                .map(provinceMapper::toProvinceResponse)
                .toList();
    }

    @Override
    @Cacheable(value = CacheName.PROVINCE_DETAIL, key = "#code")
    public ProvinceResponse getProvinceByCode(String code) {
        log.info("Cache MISS - Fetching province {} from database", code);
        Province province = provinceRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND));
        return provinceMapper.toProvinceResponse(province);
    }
}
