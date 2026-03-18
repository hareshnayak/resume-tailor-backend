package com.resumetailor.controller;

import com.resumetailor.dto.JobParseRequest;
import com.resumetailor.dto.JobParseResponse;
import com.resumetailor.model.JobDocument;
import com.resumetailor.repository.JobDocumentRepository;
import com.resumetailor.service.JobParsingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/job/parse — Scrape a job URL, save to MongoDB "jobs" collection, return jobId + text.
 * GET  /api/job/{jobId} — Retrieve a previously scraped job by its MongoDB _id.
 */
@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobParsingService jobParsingService;
    private final JobDocumentRepository jobDocumentRepository;

    /**
     * POST /api/job/parse
     *
     * Accepts a job posting URL, navigates to it with a headless browser,
     * extracts the fully rendered HTML, and parses out the job description.
     *
     * @param request Contains the jobUrl to scrape
     * @return JobParseResponse with extracted job description and title
     */
    @PostMapping("/parse")
    public ResponseEntity<JobParseResponse> parseJobUrl(
            @Valid @RequestBody JobParseRequest request) {

        log.info("Job parse request: jobUrl={}", request.getJobUrl());

        try {
            String jobText = jobParsingService.scrapeJobDescription(request.getJobUrl());
            String jobTitle = jobParsingService.extractJobTitle(request.getJobUrl());

            // Persist to MongoDB
            JobDocument saved = jobDocumentRepository.save(
                    JobDocument.builder()
                            .jobUrl(request.getJobUrl())
                            .jobTitle(jobTitle)
                            .jobDescriptionText(jobText)
                            .build()
            );

            JobParseResponse response = JobParseResponse.builder()
                    .success(true)
                    .jobId(saved.getId())
                    .jobUrl(saved.getJobUrl())
                    .jobDescriptionText(jobText)
                    .jobTitle(jobTitle)
                    .build();

            log.info("Job saved: jobId={}, {} chars", saved.getId(), jobText.length());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Job parsing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse job URL: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobParseResponse> getJob(@PathVariable String jobId) {
        log.info("Fetching job: jobId={}", jobId);

        JobDocument doc = jobDocumentRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("No job found for jobId: " + jobId));

        JobParseResponse response = JobParseResponse.builder()
                .success(true)
                .jobId(doc.getId())
                .jobUrl(doc.getJobUrl())
                .jobDescriptionText(doc.getJobDescriptionText())
                .jobTitle(doc.getJobTitle())
                .company(doc.getCompany())
                .build();

        return ResponseEntity.ok(response);
    }
}
