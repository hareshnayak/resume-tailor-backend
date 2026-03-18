package com.resumetailor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TailorRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Job description text is required")
    private String jobText;
}
