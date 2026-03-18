package com.resumetailor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TailoredVersion {

    /** Which resume version (CvDocument.currentVersion) this was tailored from */
    private int resumeVersionNumber;

    private String jobTitle;
    private String company;
    private String jobUrl;

    private String tailoredText;
    private List<Experience> tailoredExperience;
    private List<String> tailoredSkills;
    private List<String> missingKeywordsAddressed;
    private String feedback;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
