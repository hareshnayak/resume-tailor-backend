package com.resumetailor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobParseRequest {
    @NotBlank(message = "Job URL is required")
    private String jobUrl;
}
