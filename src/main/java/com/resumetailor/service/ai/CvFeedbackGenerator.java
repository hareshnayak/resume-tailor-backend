package com.resumetailor.service.ai;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CvFeedbackGenerator {

    @UserMessage("""
            Analyze the following CV and provide exactly 3 short, constructive tips on how the user can improve \
            its overall impact, formatting, or phrasing. Be specific and actionable. \
            Format each tip as a numbered item.
            
            CV Text:
            ---
            {{cvText}}
            ---
            """)
    String generateFeedback(@V("cvText") String cvText);
}

