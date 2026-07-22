package com.itjob.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING("pending"),
    REVIEWING("reviewing"),
    APPROVED("approved"),
    REJECTED("rejected"),
    WITHDRAWN("withdrawn");

    private final String value;

    ApplicationStatus(String value) {
        this.value = value;
    }

    public static ApplicationStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Application status cannot be null");
        }

        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid application status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
