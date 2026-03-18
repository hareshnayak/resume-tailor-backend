package com.resumetailor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TailorRequest {

    @NotBlank(message = "Resume ID is required. Upload a resume first via /api/resume/upload")
    private String resumeId;

    @NotBlank(message = "Job description text is required")
    private String jobDescription;
}
