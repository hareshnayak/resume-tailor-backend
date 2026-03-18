package com.resumetailor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDownloadRequest {
    @NotBlank(message = "Tailored CV text is required")
    private String tailoredCvText;

    private String candidateName;
}

