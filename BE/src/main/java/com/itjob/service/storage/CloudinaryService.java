package com.itjob.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    CloudinaryUploadResult upload(MultipartFile file, String folder);

    void delete(String publicId, String resourceType);
}