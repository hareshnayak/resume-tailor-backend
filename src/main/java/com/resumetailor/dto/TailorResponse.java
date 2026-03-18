package com.resumetailor.dto;

import com.resumetailor.model.ContactInfo;
import com.resumetailor.model.Education;
import com.resumetailor.model.Experience;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TailorResponse {

    private String resumeId;

    /** The full tailored CV as plain text */
    private String tailoredCvText;

    /** Structured fields parsed from the original resume */
    private ContactInfo contactInfo;
    private List<Experience> tailoredExperience;
    private List<Education> education;
    private List<String> tailoredSkills;

    /** Keywords the AI identified as missing from the original CV */
    private List<String> missingKeywords;

    /** Actionable feedback on the original CV */
    private String feedback;
}
