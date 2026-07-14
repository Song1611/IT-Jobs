package com.itjob.constant;

public enum JobStatus {
    OPEN("open"),
    CLOSED("closed"),
    DRAFT("draft"),
    EXPIRED("expired"),
    REJECTED("rejected");

    private final String value;

    JobStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Convert string value to enum with validation
     * 
     * @param value The status string to convert
     * @return JobStatus enum
     * @throws IllegalArgumentException if value is invalid
     */
    public static JobStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Job status cannot be null");
        }
        
        for (JobStatus status : JobStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Invalid job status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
