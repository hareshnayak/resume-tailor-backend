package com.resumetailor.controller;

import com.resumetailor.dto.JobParseRequest;
import com.resumetailor.dto.JobParseResponse;
import com.resumetailor.service.JobParsingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for parsing job postings.
 * Uses Playwright (headless Chromium) to render JS-heavy pages,
 * then Jsoup to extract the clean job description text.
 */
@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobParsingService jobParsingService;

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

        log.info("Job parse request received: jobUrl={}", request.getJobUrl());

        try {
            String jobText = jobParsingService.scrapeJobDescription(request.getJobUrl());
            String jobTitle = jobParsingService.extractJobTitle(request.getJobUrl());

            JobParseResponse response = JobParseResponse.builder()
                    .success(true)
                    .jobDescriptionText(jobText)
                    .jobTitle(jobTitle)
                    .build();

            log.info("Job description extracted successfully: {} chars", jobText.length());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Job parsing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse job URL: " + e.getMessage(), e);
        }
    }
}

