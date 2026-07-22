package com.itjob.enums;

import lombok.Getter;

@Getter
public enum ReviewStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String value;

    ReviewStatus(String value) {
        this.value = value;
    }

    public static ReviewStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Review status cannot be null");
        }

        for (ReviewStatus status : ReviewStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid review status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
