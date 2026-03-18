package com.resumetailor.service.ai;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CvAuditor {

    @UserMessage("""
            Act as an Applicant Tracking System (ATS). Compare the following CV text against the job keywords listed below. \
            Return ONLY a comma-separated list of keywords/skills that are MISSING or UNDERREPRESENTED in the CV. \
            Do not include keywords that already appear in the CV.
            
            CV Text:
            ---
            {{cvText}}
            ---
            
            Job Keywords:
            ---
            {{keywords}}
            ---
            """)
    String auditCv(@V("cvText") String cvText, @V("keywords") String keywords);
}

