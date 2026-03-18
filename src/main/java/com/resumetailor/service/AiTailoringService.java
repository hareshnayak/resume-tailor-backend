package com.resumetailor.service;

import com.resumetailor.dto.TailorResponse;
import com.resumetailor.model.CvDocument;
import com.resumetailor.service.ai.CvAuditor;
import com.resumetailor.service.ai.CvFeedbackGenerator;
import com.resumetailor.service.ai.CvTailor;
import com.resumetailor.service.ai.KeywordExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTailoringService {

    private final KeywordExtractor keywordExtractor;
    private final CvAuditor cvAuditor;
    private final CvTailor cvTailor;
    private final CvFeedbackGenerator cvFeedbackGenerator;

    /**
     * Orchestrates the full AI tailoring pipeline against a stored resume.
     *
     * 1. Extract keywords from job description
     * 2. Audit CV against extracted keywords → list of missing keywords
     * 3. Rewrite/tailor the CV text
     * 4. Generate feedback on the original CV
     *
     * @param cvDocument     The full resume document fetched from MongoDB
     * @param jobDescription The raw job description text
     * @return Structured TailorResponse with tailored text, structured fields,
     *         missing keywords, and feedback
     */
    public TailorResponse tailorCv(CvDocument cvDocument, String jobDescription) {
        String cvText = cvDocument.getOriginalText();
        log.info("Starting AI tailoring pipeline for resumeId={}...", cvDocument.getId());

        // Step 1: Extract keywords from the job description
        log.info("Step 1/4: Extracting keywords from job description...");
        String keywords = keywordExtractor.extractKeywords(jobDescription);
        log.debug("Extracted keywords: {}", keywords);

        // Step 2: Audit CV against extracted keywords
        log.info("Step 2/4: Auditing CV against job keywords...");
        String missingKeywordsRaw = cvAuditor.auditCv(cvText, keywords);
        log.debug("Missing keywords: {}", missingKeywordsRaw);

        // Parse comma-separated missing keywords into a list
        List<String> missingKeywords = Arrays.stream(missingKeywordsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Step 3: Tailor/rewrite the CV text
        log.info("Step 3/4: Tailoring CV with missing keywords...");
        String tailoredCvText = cvTailor.tailorCv(cvText, missingKeywordsRaw, jobDescription);
        log.debug("Tailored CV generated ({} chars)", tailoredCvText.length());

        // Step 4: Generate general feedback on the original CV
        log.info("Step 4/4: Generating feedback on original CV...");
        String feedback = cvFeedbackGenerator.generateFeedback(cvText);
        log.debug("Feedback generated");

        log.info("AI tailoring pipeline completed successfully");

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
