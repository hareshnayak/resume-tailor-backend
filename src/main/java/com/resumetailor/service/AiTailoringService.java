package com.resumetailor.service;

import com.resumetailor.dto.TailorResponse;
import com.resumetailor.model.CvDocument;
import com.resumetailor.service.ai.CvFeedbackGenerator;
import com.resumetailor.service.ai.CvTailor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTailoringService {

    // ── Programmatic (no AI) ───────────────────────────────────────────────
    private final KeywordExtractorService keywordExtractorService;
    private final CvAuditorService cvAuditorService;

    // ── AI-powered (LangChain4j @AiService) ────────────────────────────────
    private final CvTailor cvTailor;
    private final CvFeedbackGenerator cvFeedbackGenerator;

    /**
     * Orchestrates the full tailoring pipeline against a stored resume.
     *
     * Steps 1 & 2 are pure text processing (no LLM calls):
     *   1. Extract keywords from job description  → dictionary + pattern matching
     *   2. Audit CV against extracted keywords     → diff / word-boundary check
     *
     * Steps 3 & 4 still use the AI model:
     *   3. Rewrite/tailor the CV text
     *   4. Generate feedback on the original CV
     *
     * @param cvDocument     The full resume document fetched from MongoDB
     * @param jobDescription The raw job description text
     * @return Structured TailorResponse
     */
    public TailorResponse tailorCv(CvDocument cvDocument, String jobDescription) {
        String cvText = cvDocument.getOriginalText();
        log.info("Starting tailoring pipeline for resumeId={}...", cvDocument.getId());

        // Step 1: Extract keywords (programmatic — no AI)
        log.info("Step 1/4: Extracting keywords from job description (dictionary match)...");
        List<String> keywords = keywordExtractorService.extractKeywords(jobDescription);
        log.info("Extracted {} keywords", keywords.size());

        // Step 2: Audit CV for missing keywords (programmatic — no AI)
        log.info("Step 2/4: Auditing CV against job keywords (text diff)...");
        List<String> missingKeywords = cvAuditorService.findMissingKeywords(cvText, keywords);
        log.info("{} keywords missing from CV", missingKeywords.size());

        // Build a comma-separated string for the AI tailor prompt
        String missingKeywordsCsv = String.join(", ", missingKeywords);

        // Step 3: Tailor/rewrite the CV (AI)
        log.info("Step 3/4: Tailoring CV with missing keywords (AI)...");
        String tailoredCvText = cvTailor.tailorCv(cvText, missingKeywordsCsv, jobDescription);
        log.debug("Tailored CV generated ({} chars)", tailoredCvText.length());

        // Step 4: Generate feedback on the original CV (AI)
        log.info("Step 4/4: Generating feedback on original CV (AI)...");
        String feedback = cvFeedbackGenerator.generateFeedback(cvText);
        log.debug("Feedback generated");

        log.info("Tailoring pipeline completed successfully");

        return TailorResponse.builder()
                .resumeId(cvDocument.getId())
                .tailoredCvText(tailoredCvText)
                .contactInfo(cvDocument.getContactInfo())
                .tailoredExperience(cvDocument.getExperience())
                .education(cvDocument.getEducation())
                .tailoredSkills(cvDocument.getSkills())
                .missingKeywords(missingKeywords)
                .feedback(feedback)
                .build();
    }
}
