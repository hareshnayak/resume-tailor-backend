package com.resumetailor.service;

import com.resumetailor.dto.TailorResponse;
import com.resumetailor.service.ai.CvAuditor;
import com.resumetailor.service.ai.CvFeedbackGenerator;
import com.resumetailor.service.ai.CvTailor;
import com.resumetailor.service.ai.KeywordExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTailoringService {

    private final KeywordExtractor keywordExtractor;
    private final CvAuditor cvAuditor;
    private final CvTailor cvTailor;
    private final CvFeedbackGenerator cvFeedbackGenerator;

    /**
     * Orchestrates the full AI tailoring pipeline:
     * 1. Extract keywords from job description
     * 2. Audit CV against extracted keywords
     * 3. Rewrite/tailor the CV
     * 4. Generate feedback on the original CV
     */
    public TailorResponse tailorCv(String cvText, String jobText) {
        log.info("Starting AI tailoring pipeline...");

        // Step 1: Extract keywords from the job description
        log.info("Step 1/4: Extracting keywords from job description...");
        String keywords = keywordExtractor.extractKeywords(jobText);
        log.debug("Extracted keywords: {}", keywords);

        // Step 2: Audit CV against extracted keywords
        log.info("Step 2/4: Auditing CV against job keywords...");
        String missingKeywords = cvAuditor.auditCv(cvText, keywords);
        log.debug("Missing keywords: {}", missingKeywords);

        // Step 3: Tailor/rewrite the CV
        log.info("Step 3/4: Tailoring CV with missing keywords...");
        String tailoredCvText = cvTailor.tailorCv(cvText, missingKeywords, jobText);
        log.debug("Tailored CV generated ({} chars)", tailoredCvText.length());

        // Step 4: Generate general feedback on the original CV
        log.info("Step 4/4: Generating feedback on original CV...");
        String feedback = cvFeedbackGenerator.generateFeedback(cvText);
        log.debug("Feedback generated");

        log.info("AI tailoring pipeline completed successfully");

        return TailorResponse.builder()
                .tailoredCvText(tailoredCvText)
                .feedback(feedback)
                .build();
    }
}
