package com.resumetailor.service.ai;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CvTailor {

    @UserMessage("""
            You are a professional resume writer. Rewrite the provided CV text to better align with the target job description. \
            Incorporate the missing keywords where logically applicable based on the user's existing experience.
            
            IMPORTANT RULES:
            - Do NOT invent fake experience or lie about qualifications.
            - Maintain the original structure and factual accuracy.
            - Naturally weave in relevant keywords and phrases.
            - Improve action verbs and quantify achievements where possible.
            - Keep the tone professional and concise.
            
            Original CV Text:
            ---
            {{cvText}}
            ---
            
            Missing Keywords to incorporate:
            ---
            {{missingKeywords}}
            ---
            
            Job Description for context:
            ---
            {{jobText}}
            ---
            
            Return only the rewritten CV text.
            """)
    String tailorCv(@V("cvText") String cvText,
                    @V("missingKeywords") String missingKeywords,
                    @V("jobText") String jobText);
}

