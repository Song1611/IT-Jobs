package com.itjob.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ViewEntity {

    JOB("job"),
    COMPANY("company"),
    BLOG("blog"),
    POST("post");

    private final String key;
}
