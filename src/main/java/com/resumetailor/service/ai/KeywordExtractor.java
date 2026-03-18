package com.resumetailor.service.ai;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface KeywordExtractor {

    @UserMessage("""
            Analyze the following job description and return a structured, comma-separated list of the most critical \
            technical skills, soft skills, and requirements. Only return the comma-separated list, nothing else.
            
            Job Description:
            ---
            {{jobText}}
            ---
            """)
    String extractKeywords(@V("jobText") String jobText);
}

