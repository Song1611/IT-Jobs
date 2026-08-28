package com.itjob.integration.service;

import com.itjob.entity.Province;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.ProvinceRepository;
import com.itjob.service.ProvinceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - ProvinceService")
class ProvinceServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private ProvinceService provinceService;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Test
    @DisplayName("getAllProvinces -> returns all saved provinces")
    void getAllProvincesReturnsSaved() {
        provinceRepository.save(province());

        var result = provinceService.getAllProvinces();

        assertThat(result).extracting("code").contains("HCM");
    }

    @Test
    @DisplayName("getProvinceByCode -> returns the province for a valid code")
    void getProvinceByCodeReturnsProvince() {
        provinceRepository.save(province());

        var result = provinceService.getProvinceByCode("HCM");

        assertThat(result.getName()).isEqualTo("Ho Chi Minh");
    }

    @Test
    @DisplayName("getProvinceByCode -> throws PROVINCE_NOT_FOUND for an unknown code")
    void getProvinceByCodeNotFoundThrows() {
        assertThatThrownBy(() -> provinceService.getProvinceByCode("XXX"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROVINCE_NOT_FOUND);
    }

    private Province province() {
        Province province = new Province();
        province.setCode("HCM");
        province.setName("Ho Chi Minh");
        province.setFullName("Ho Chi Minh");
        return province;
    }
}