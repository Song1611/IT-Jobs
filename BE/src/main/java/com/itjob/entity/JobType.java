package com.itjob.entity;

public enum JobType {
    FULL_TIME("full-time"),
    PART_TIME("part-time"),
    CONTRACT("contract"),
    INTERNSHIP("internship"),
    FREELANCE("freelance");
    
    private final String value;
    
    JobType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
