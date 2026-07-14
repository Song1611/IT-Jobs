package com.itjob.enums;

import lombok.Getter;

@Getter
public enum CompanyStatus {
    ACTIVE("active"),
    PENDING("pending"),
    REJECTED("rejected"),
    SUSPENDED("suspended");

    private final String value;

    CompanyStatus(String value) {
        this.value = value;
    }

    /**
     * Convert string value to enum with validation
     * Throws IllegalArgumentException for invalid status values
     * 
     * @param value The status string to convert
     * @return CompanyStatus enum
     * @throws IllegalArgumentException if value is invalid
     */
    public static CompanyStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Company status cannot be null");
        }
        
        for (CompanyStatus status : CompanyStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Invalid company status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
