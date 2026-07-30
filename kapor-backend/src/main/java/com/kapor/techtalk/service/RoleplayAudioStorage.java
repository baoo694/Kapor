package com.kapor.techtalk.service;

import com.kapor.common.exception.ResourceNotFoundException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleplayAudioStorage {
    private final MinioClient minioClient;
    @Value("${minio.bucket-name}") private String bucketName;

    public String store(String userId, byte[] wav) {
        String key = "techtalk/recordings/" + userId + "/" + UUID.randomUUID() + ".wav";
        try {
            ensureBucket();
            try (InputStream input = new ByteArrayInputStream(wav)) {
                minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(key)
                        .stream(input, wav.length, -1).contentType("audio/wav").build());
            }
            return key;
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể lưu bản ghi TechTalk an toàn.", exception);
        }
    }

    public byte[] read(String key) {
        try (InputStream input = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(key).build())) {
            return input.readAllBytes();
        } catch (Exception exception) {
            throw new ResourceNotFoundException("TechTalk recording", "objectKey", key);
        }
    }

    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) return;
        try {
            minioClient.removeObject(io.minio.RemoveObjectArgs.builder().bucket(bucketName).object(key).build());
        } catch (Exception exception) {
            log.warn("Could not remove TechTalk recording {}", key);
        }
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            minioClient.removeObject(io.minio.RemoveObjectArgs.builder()
                    .bucket(bucketName).object(key).build());
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể xoá bản ghi TechTalk hết hạn.", exception);
        }
    }

    private void ensureBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }
}
