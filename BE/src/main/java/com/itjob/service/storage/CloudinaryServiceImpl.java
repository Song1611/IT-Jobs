package com.itjob.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    @SuppressWarnings("unchecked")
    public CloudinaryUploadResult upload(MultipartFile file, String folder) {

        String resourceType = getString(file);

        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", UUID.randomUUID().toString(),
                    "resource_type", resourceType
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader()
                    .upload(file.getBytes(), params);

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary did not return secure URL");
            }

            return new CloudinaryUploadResult(secureUrl, publicId, resourceType);

        } catch (Exception e) {
            log.error(
                    "Cloudinary upload failed. filename={}, folder={}",
                    file.getOriginalFilename(),
                    folder,
                    e
            );

            throw new RuntimeException("Failed to upload file", e);
        }
    }

    private static String getString(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException("File type is unknown");
        }

        String resourceType;

        if (contentType.startsWith("image/")) {
            resourceType = "image";
        } else if (contentType.startsWith("video/")) {
            resourceType = "video";
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType
            );
        }
        return resourceType;
    }

    @Override
    public void delete(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", resourceType
                    )
            );
        } catch (Exception e) {
            log.error(
                    "Cloudinary delete failed. publicId={}",
                    publicId,
                    e
            );

            throw new RuntimeException("Failed to delete file", e);
        }
    }
}