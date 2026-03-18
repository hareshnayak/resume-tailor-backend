package com.resumetailor.controller;

import com.resumetailor.dto.CvUploadResponse;
import com.resumetailor.model.CvDocument;
import com.resumetailor.service.CvUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for uploading and parsing CVs.
 * Extracts text via Apache Tika, parses into structured fields,
 * and stores the result in the MongoDB "resumes" collection.
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResumeController {

    private final CvUploadService cvUploadService;

    /**
     * POST /api/resume/upload
     *
     * Accepts a CV file (PDF/DOCX) and a userId.
     * Extracts text, parses into structured fields (contact, experience,
     * education, skills), and saves to MongoDB.
     *
     * @param file   The CV file (PDF, DOCX, etc.)
     * @param userId A unique user identifier
     * @return CvUploadResponse with the parsed resume data
     */
    @PostMapping("/upload")
    public ResponseEntity<CvUploadResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {

        log.info("Resume upload request: file={}, size={}, userId={}",
                file.getOriginalFilename(), file.getSize(), userId);

        try {
            CvDocument saved = cvUploadService.processAndStore(file, userId);

            CvUploadResponse response = CvUploadResponse.builder()
                    .success(true)
                    .message("Resume uploaded and parsed successfully (version " + saved.getCurrentVersion() + ")")
                    .resumeId(saved.getId())
                    .userId(saved.getUserId())
                    .currentVersion(saved.getCurrentVersion())
                    .totalVersions(saved.getVersions().size() + 1) // history + current
                    .contactInfo(saved.getContactInfo())
                    .experience(saved.getExperience())
                    .education(saved.getEducation())
                    .skills(saved.getSkills())
                    .build();

            log.info("Resume stored successfully: resumeId={}", saved.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Resume upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process CV: " + e.getMessage(), e);
        }
    }

    /**
     * GET /api/resume/{resumeId}
     *
     * Retrieves the stored resume for a given user.
     */
    @GetMapping("/{resumeId}")
    public ResponseEntity<CvUploadResponse> getResume(@PathVariable String resumeId) {
        log.info("Fetching resume: resumeId={}", resumeId);

        CvDocument doc = cvUploadService.findById(resumeId);

        CvUploadResponse response = CvUploadResponse.builder()
                .success(true)
                .message("Resume retrieved successfully")
                .resumeId(doc.getId())
                .userId(doc.getUserId())
                .currentVersion(doc.getCurrentVersion())
                .totalVersions(doc.getVersions().size() + 1)
                .contactInfo(doc.getContactInfo())
                .experience(doc.getExperience())
                .education(doc.getEducation())
                .skills(doc.getSkills())
                .build();

        return ResponseEntity.ok(response);
    }
}
