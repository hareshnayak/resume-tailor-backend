package com.resumetailor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TailoredVersion {
    private String jobTitle;
    private String company;
    private String tailoredText;
    private List<Experience> tailoredExperience;
    private List<String> missingKeywordsAddressed;
    private String feedback;
    private String jobUrl;
}
