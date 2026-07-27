package com.itjob.service;

import com.itjob.dto.response.ProvinceResponse;

import java.util.List;

public interface ProvinceService {

    List<ProvinceResponse> getAllProvinces();

    ProvinceResponse getProvinceByCode(String code);
}
