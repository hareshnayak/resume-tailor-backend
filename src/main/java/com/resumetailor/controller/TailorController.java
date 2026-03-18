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
 * POST /api/tailor — Submit resumeId + jobDescription.
 * Fetches the stored resume, runs the AI tailoring pipeline,
 * saves a tailored version to MongoDB, and returns structured JSON + feedback.
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
     * Accepts a resumeId and job description text.
     * Retrieves the resume from MongoDB by _id, runs the AI tailoring
     * pipeline (keyword extraction → audit → tailor → feedback),
     * saves the tailored version back to MongoDB, and returns the result.
     *
     * @param request Contains resumeId and jobDescription
     * @return TailorResponse with tailored CV text and feedback
     */
    @PostMapping
    public ResponseEntity<TailorResponse> tailorResume(
            @Valid @RequestBody TailorRequest request) {

        log.info("Tailor request: resumeId={}", request.getResumeId());

        try {
            // Step 1: Fetch stored resume from MongoDB by _id
            CvDocument cvDocument = cvDocumentRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No resume found for resumeId: " + request.getResumeId()
                                    + ". Upload a resume first via POST /api/resume/upload"));

            log.info("Retrieved resume: resumeId={}, version={}, {} chars",
                    cvDocument.getId(), cvDocument.getCurrentVersion(),
                    cvDocument.getOriginalText().length());

            // Step 2: Run the AI tailoring pipeline
            TailorResponse response = aiTailoringService.tailorCv(cvDocument, request.getJobDescription());

            // Step 3: Save the tailored version back to MongoDB
            TailoredVersion version = TailoredVersion.builder()
                    .resumeVersionNumber(cvDocument.getCurrentVersion())
                    .tailoredText(response.getTailoredCvText())
                    .tailoredExperience(response.getTailoredExperience())
                    .tailoredSkills(response.getTailoredSkills())
                    .missingKeywordsAddressed(response.getMissingKeywords())
                    .feedback(response.getFeedback())
                    .createdAt(Instant.now())
                    .build();

            cvDocument.getTailoredVersions().add(version);
            cvDocument.setLastUpdated(Instant.now());
            cvDocumentRepository.save(cvDocument);

            log.info("Tailored version #{} saved for resumeId={}",
                    cvDocument.getTailoredVersions().size(), cvDocument.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Resume tailoring failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to tailor resume: " + e.getMessage(), e);
        }
    }
}
