package com.resumetailor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single historical snapshot of a resume upload.
 * Each time a user re-uploads their CV, the previous state
 * is preserved as a ResumeVersion in the versions list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVersion {

    private int versionNumber;

    private String originalText;

    private ContactInfo contactInfo;

    @Builder.Default
    private List<Experience> experience = new ArrayList<>();

    @Builder.Default
    private List<Education> education = new ArrayList<>();

    @Builder.Default
    private List<String> skills = new ArrayList<>();

    private String originalFileName;
    private String fileType;

    @Builder.Default
    private Instant uploadedAt = Instant.now();
}

