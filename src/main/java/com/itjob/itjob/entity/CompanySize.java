package com.itjob.itjob.entity;

public enum CompanySize {
    SIZE_1_50("1-50"),
    SIZE_51_200("51-200"),
    SIZE_201_500("201-500"),
    SIZE_501_1000("501-1000"),
    SIZE_1000_PLUS("1000+");
    
    private final String value;
    
    CompanySize(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
