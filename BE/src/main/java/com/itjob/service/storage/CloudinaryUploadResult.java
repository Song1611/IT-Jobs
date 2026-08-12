package com.itjob.service.storage;

public record CloudinaryUploadResult(
        String url,
        String publicId,
        String resourceType
) {
}