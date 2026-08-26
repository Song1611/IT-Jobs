package com.itjob.unit.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.storage.CloudinaryServiceImpl;
import com.itjob.service.storage.CloudinaryUploadResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - CloudinaryServiceImpl")
class CloudinaryServiceImplTest {

    private static final byte[] FILE_BYTES = {1, 2, 3};

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @Test
    @DisplayName("upload image -> returns result with image resource type and folder param")
    void uploadImageReturnsResult() throws Exception {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(FILE_BYTES);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/x/image/upload/abc.png",
                "public_id", "abc"));

        // Act
        CloudinaryUploadResult result = cloudinaryService.upload(file, "avatars");

        // Assert
        assertThat(result.url()).isEqualTo("https://res.cloudinary.com/x/image/upload/abc.png");
        assertThat(result.publicId()).isEqualTo("abc");
        assertThat(result.resourceType()).isEqualTo("image");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(FILE_BYTES), params.capture());
        assertThat(params.getValue())
                .containsEntry("folder", "avatars")
                .containsEntry("resource_type", "image");
    }

    @Test
    @DisplayName("upload video -> returns result with video resource type and folder param")
    void uploadVideoUsesVideoType() throws Exception {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("video/mp4");
        when(file.getBytes()).thenReturn(FILE_BYTES);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/x/video/upload/v1.mp4",
                "public_id", "v1"));

        // Act
        CloudinaryUploadResult result = cloudinaryService.upload(file, "media");

        // Assert
        assertThat(result.resourceType()).isEqualTo("video");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(FILE_BYTES), params.capture());
        assertThat(params.getValue())
                .containsEntry("folder", "media")
                .containsEntry("resource_type", "video");
    }

    @Test
    @DisplayName("upload raw file -> returns result with raw resource type and folder param")
    void uploadRawUsesRawType() throws Exception {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn(FILE_BYTES);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/x/raw/upload/doc.pdf",
                "public_id", "doc"));

        // Act
        CloudinaryUploadResult result = cloudinaryService.upload(file, "documents");

        // Assert
        assertThat(result.resourceType()).isEqualTo("raw");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(FILE_BYTES), params.capture());
        assertThat(params.getValue())
                .containsEntry("folder", "documents")
                .containsEntry("resource_type", "raw");
    }

    @Test
    @DisplayName("upload null file -> IllegalArgumentException")
    void uploadNullFileRejects() {
        // Act & Assert
        assertThatThrownBy(() -> cloudinaryService.upload(null, "avatars"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File is empty");
    }

    @Test
    @DisplayName("upload empty file -> IllegalArgumentException")
    void uploadEmptyFileRejects() {
        // Arrange
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> cloudinaryService.upload(file, "avatars"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File is empty");
    }

    @Test
    @DisplayName("upload null content type -> IllegalArgumentException")
    void uploadUnknownContentTypeRejects() {
        // Arrange
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> cloudinaryService.upload(file, "avatars"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File type is unknown");
    }

    @Test
    @DisplayName("upload cloudinary failure -> throws FILE_UPLOAD_FAILED")
    void uploadCloudinaryFailureThrows() throws Exception {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(FILE_BYTES);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("boom"));

        // Act & Assert
        assertThatThrownBy(() -> cloudinaryService.upload(file, "avatars"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("upload without secure URL -> throws FILE_UPLOAD_FAILED")
    void uploadMissingSecureUrlThrows() throws Exception {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(FILE_BYTES);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("public_id", "abc"));

        // Act & Assert
        assertThatThrownBy(() -> cloudinaryService.upload(file, "avatars"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("delete blank public id -> does nothing")
    void deleteBlankPublicIdDoesNothing() {
        // Act
        cloudinaryService.delete("   ", "image");

        // Assert
        verifyNoInteractions(cloudinary, uploader);
    }

    @Test
    @DisplayName("delete null public id -> does nothing")
    void deleteNullPublicIdDoesNothing() {
        // Act
        cloudinaryService.delete(null, "image");

        // Assert
        verifyNoInteractions(cloudinary, uploader);
    }

    @Test
    @DisplayName("delete -> destroys resource with resource type param")
    void deleteDestroysResource() throws Exception {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);

        // Act
        cloudinaryService.delete("abc", "image");

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(eq("abc"), params.capture());
        assertThat(params.getValue()).containsEntry("resource_type", "image");
    }

    @Test
    @DisplayName("delete cloudinary failure -> throws FILE_DELETE_FAILED")
    void deleteCloudinaryFailureThrows() throws Exception {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("boom"));

        // Act & Assert
        assertThatThrownBy(() -> cloudinaryService.delete("abc", "image"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_DELETE_FAILED);
    }
}
