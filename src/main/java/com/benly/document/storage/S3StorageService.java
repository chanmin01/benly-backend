package com.benly.document.storage;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final String KEY_FORMAT = "documents/%d/%s.pdf";

    private final S3Template s3Template;

    @Value("${benly.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, Long userId) {
        String key = KEY_FORMAT.formatted(userId, UUID.randomUUID());

        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .contentLength(file.getSize())
                .build();

        try {
            s3Template.upload(bucket, key, file.getInputStream(), metadata);
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
        return key;
    }

    public void delete(String key) {
        s3Template.deleteObject(bucket, key);
    }


}
