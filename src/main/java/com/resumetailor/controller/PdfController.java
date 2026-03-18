package com.resumetailor.controller;

import com.resumetailor.dto.ResumeDownloadRequest;
import com.resumetailor.dto.ResumeDownloadResponse;
import com.resumetailor.service.PdfGenerationService;
import com.resumetailor.service.S3StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for PDF generation and download.
 * Generates a professional PDF from tailored CV text,
 * uploads it to AWS S3, and returns a pre-signed download URL.
 */
@Slf4j
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PdfController {

    private final PdfGenerationService pdfGenerationService;
    private final S3StorageService s3StorageService;

    @Value("${aws.s3.presigned-url-expiry-minutes:60}")
    private long presignedUrlExpiryMinutes;

    /**
     * POST /api/pdf/download
     *
     * Accepts tailored CV text and a candidate name.
     * Generates a professionally formatted PDF, uploads it to AWS S3,
     * and returns a pre-signed downloadable URL.
     *
     * @param request Contains tailoredCvText and candidateName
     * @return ResumeDownloadResponse with the S3 download URL
     */
    @PostMapping("/download")
    public ResponseEntity<ResumeDownloadResponse> downloadResume(
            @Valid @RequestBody ResumeDownloadRequest request) {

        log.info("PDF download request received for candidate: {}", request.getCandidateName());

        try {
            // Step 1: Generate PDF from tailored CV text
            log.info("Step 1: Generating PDF...");
            byte[] pdfBytes = pdfGenerationService.generatePdf(
                    request.getTailoredCvText(),
                    request.getCandidateName()
            );
            log.info("PDF generated: {} bytes", pdfBytes.length);

            // Step 2: Upload PDF to S3 and get a pre-signed download URL
            log.info("Step 2: Uploading PDF to S3...");
            String downloadUrl = s3StorageService.uploadPdfAndGetDownloadUrl(
                    pdfBytes,
                    request.getCandidateName()
            );

            ResumeDownloadResponse response = ResumeDownloadResponse.builder()
                    .downloadUrl(downloadUrl)
                    .message("PDF generated and uploaded successfully")
                    .expiresInMinutes(presignedUrlExpiryMinutes)
                    .build();

            log.info("PDF download URL generated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("PDF download failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate and upload PDF: " + e.getMessage(), e);
        }
    }
}

