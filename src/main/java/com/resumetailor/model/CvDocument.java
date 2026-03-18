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

    private String originalText;

    private ContactInfo contactInfo;

    @Builder.Default
    private List<Experience> experience = new ArrayList<>();

    @Builder.Default
    private List<Education> education = new ArrayList<>();

    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Builder.Default
    private List<TailoredVersion> tailoredVersions = new ArrayList<>();

    private String originalFileName;
    private String fileType;

    @Builder.Default
    private Instant lastUpdated = Instant.now();
}
