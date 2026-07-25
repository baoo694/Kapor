package com.kapor.pronunciation.service;

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

/** Keeps learner recordings private: objects are never published or exposed by URL. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationAudioStorage {
    private final MinioClient minioClient;
    @Value("${minio.bucket-name}") private String bucketName;

    public String storeAttempt(String userId, byte[] wav) {
        String objectKey = "pronunciation/attempts/" + userId + "/" + UUID.randomUUID() + ".wav";
        try {
            ensureBucket();
            try (InputStream stream = new ByteArrayInputStream(wav)) {
                minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectKey)
                        .stream(stream, wav.length, -1).contentType("audio/wav").build());
            }
            return objectKey;
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể lưu bản ghi phát âm an toàn.", exception);
        }
    }

    public AudioData read(String objectKey) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectKey).build())) {
            return new AudioData(stream.readAllBytes(), "audio/wav");
        } catch (Exception exception) {
            throw new ResourceNotFoundException("Pronunciation recording", "objectKey", objectKey);
        }
    }

    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            minioClient.removeObject(io.minio.RemoveObjectArgs.builder().bucket(bucketName).object(objectKey).build());
        } catch (Exception exception) {
            log.warn("Could not remove expired pronunciation recording {}", objectKey, exception);
        }
    }

    private void ensureBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    public record AudioData(byte[] bytes, String contentType) { }
}
