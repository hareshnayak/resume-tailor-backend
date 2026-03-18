package com.resumetailor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobParseResponse {
    private boolean success;
    private String jobDescriptionText;
    private String jobTitle;
    private String company;
}
