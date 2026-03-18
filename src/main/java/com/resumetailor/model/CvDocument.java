package com.resumetailor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resumes")
public class CvDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    /** Current/active version number (starts at 1, increments on each re-upload) */
    @Builder.Default
    private int currentVersion = 1;

    // ── Latest (active) resume fields ──────────────────────────

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

    // ── Version history ────────────────────────────────────────

    /** Complete history of all resume uploads (oldest → newest) */
    @Builder.Default
    private List<ResumeVersion> versions = new ArrayList<>();

    // ── Tailored versions (shared across all resume versions) ──

    @Builder.Default
    private List<TailoredVersion> tailoredVersions = new ArrayList<>();

    @Builder.Default
    private Instant lastUpdated = Instant.now();
}
