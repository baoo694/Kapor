package com.kapor.tts;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

/**
 * A best-effort persistent cache for generated pronunciation audio.
 *
 * A cache outage must not prevent a learner from hearing a word, so read and
 * write failures are logged and synthesis continues without persistence.
 */
@Slf4j
@Service
public class TtsAudioCache {

    private final MinioClient minioClient;
    private final String bucketName;

    public TtsAudioCache(
            MinioClient minioClient,
            @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public Optional<byte[]> get(String objectName) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build())) {
            return Optional.of(stream.readAllBytes());
        } catch (ErrorResponseException exception) {
            if (isMissingObject(exception)) return Optional.empty();
            log.warn("Unable to read TTS audio cache '{}': {}", objectName, exception.getMessage());
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("Unable to read TTS audio cache '{}': {}", objectName, exception.getMessage());
            return Optional.empty();
        }
    }

    public void put(String objectName, byte[] audio) {
        try {
            ensureBucketExists();
            try (InputStream stream = new ByteArrayInputStream(audio)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(stream, audio.length, -1)
                        .contentType("audio/wav")
                        .build());
            }
        } catch (Exception exception) {
            log.warn("Unable to write TTS audio cache '{}': {}", objectName, exception.getMessage());
        }
    }

    private void ensureBucketExists() throws Exception {
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) return;
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    }

    private boolean isMissingObject(ErrorResponseException exception) {
        String code = exception.errorResponse().code();
        return "NoSuchKey".equals(code)
                || "NoSuchObject".equals(code)
                || "NoSuchBucket".equals(code);
    }
}
