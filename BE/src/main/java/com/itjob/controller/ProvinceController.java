package com.itjob.controller;

import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.ProvinceResponse;
import com.itjob.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provinces")
@RequiredArgsConstructor
@Slf4j
public class ProvinceController {

    private final ProvinceService provinceService;

    @GetMapping
    public ApiResponse<List<ProvinceResponse>> getAllProvinces() {
        log.info("Get all provinces request");
        return ApiResponse.<List<ProvinceResponse>>builder()
                .result(provinceService.getAllProvinces())
                .build();
    }

    @GetMapping("/{code}")
    public ApiResponse<ProvinceResponse> getProvinceByCode(@PathVariable String code) {
        log.info("Get province by code: {}", code);
        return ApiResponse.<ProvinceResponse>builder()
                .result(provinceService.getProvinceByCode(code))
                .build();
    }
}
