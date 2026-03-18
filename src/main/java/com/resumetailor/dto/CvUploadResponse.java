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
public class CvUploadResponse {
    private boolean success;
    private String message;
    private String cvId;
    private String userId;
    private ContactInfo contactInfo;
    private List<Experience> experience;
    private List<Education> education;
    private List<String> skills;
}
