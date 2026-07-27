package com.itjob.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProvinceResponse {
    String code;
    String name;
    String fullName;
    String nameEn;
    String fullNameEn;
}
