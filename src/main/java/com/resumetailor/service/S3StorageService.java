package com.resumetailor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry-minutes:60}")
    private long presignedUrlExpiryMinutes;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Uploads a PDF byte array to S3 and returns a pre-signed download URL.
     *
     * @param pdfBytes      The PDF content as bytes
     * @param candidateName The candidate name (used in the S3 key)
     * @return Pre-signed download URL valid for the configured expiry time
     */
    public String uploadPdfAndGetDownloadUrl(byte[] pdfBytes, String candidateName) {
        String sanitizedName = sanitizeFileName(candidateName);
        String s3Key = "tailored-resumes/" + sanitizedName + "_" + UUID.randomUUID() + ".pdf";

        log.info("Uploading PDF to S3: bucket={}, key={}, size={} bytes", bucketName, s3Key, pdfBytes.length);

        // Upload to S3
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("application/pdf")
                .contentDisposition("attachment; filename=\"" + sanitizedName + "_tailored_resume.pdf\"")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(pdfBytes));
        log.info("PDF uploaded successfully to S3: {}", s3Key);

        // Generate pre-signed download URL
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String downloadUrl = presignedRequest.url().toString();

        log.info("Generated pre-signed download URL (expires in {} min): {}", presignedUrlExpiryMinutes, downloadUrl);
        return downloadUrl;
    }

    /**
     * Sanitizes a candidate name into a safe file name component.
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "resume";
        }
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_")
                .toLowerCase()
                .substring(0, Math.min(name.length(), 50));
    }
}

