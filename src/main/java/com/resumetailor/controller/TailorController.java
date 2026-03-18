package com.resumetailor.controller;

import com.resumetailor.dto.TailorRequest;
import com.resumetailor.dto.TailorResponse;
import com.resumetailor.model.CvDocument;
import com.resumetailor.model.TailoredVersion;
import com.resumetailor.repository.CvDocumentRepository;
import com.resumetailor.service.AiTailoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Controller for AI-powered resume tailoring.
 * Fetches the stored CV from MongoDB, runs it through the AI tailoring
 * pipeline against a job description, and returns tailored text + feedback.
 */
@Slf4j
@RestController
@RequestMapping("/api/tailor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TailorController {

    private final AiTailoringService aiTailoringService;
    private final CvDocumentRepository cvDocumentRepository;

    /**
     * POST /api/tailor
     *
     * Accepts a userId and job description text.
     * Retrieves the user's stored CV from MongoDB, runs the AI tailoring
     * pipeline (keyword extraction → audit → tailor → feedback),
     * saves the tailored version back to MongoDB, and returns the result.
     *
     * @param request Contains userId and jobText
     * @return TailorResponse with tailored CV text and feedback
     */
    @PostMapping
    public ResponseEntity<TailorResponse> tailorResume(
            @Valid @RequestBody TailorRequest request) {

        log.info("Tailor request received: userId={}", request.getUserId());

        try {
            // Step 1: Fetch stored CV from MongoDB
            CvDocument cvDocument = cvDocumentRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No resume found for userId: " + request.getUserId()
                                    + ". Please upload a resume first via /api/resume/upload"));

            String cvText = cvDocument.getOriginalText();
            log.info("Retrieved stored CV for user: {} ({} chars)", request.getUserId(), cvText.length());

            // Step 2: Run the AI tailoring pipeline
            log.info("Running AI tailoring pipeline...");
            TailorResponse response = aiTailoringService.tailorCv(cvText, request.getJobText());

            // Step 3: Save the tailored version back to MongoDB
            TailoredVersion version = TailoredVersion.builder()
                    .tailoredText(response.getTailoredCvText())
                    .feedback(response.getFeedback())
                    .build();

            cvDocument.getTailoredVersions().add(version);
            cvDocument.setLastUpdated(Instant.now());
            cvDocumentRepository.save(cvDocument);
            log.info("Tailored version saved to MongoDB for user: {}", request.getUserId());

            log.info("Tailoring completed successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            throw e; // let GlobalExceptionHandler handle it as 400
        } catch (Exception e) {
            log.error("Resume tailoring failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to tailor resume: " + e.getMessage(), e);
        }
    }
}

