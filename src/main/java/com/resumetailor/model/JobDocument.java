package com.resumetailor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persisted job posting scraped from a URL.
 * Stored in the "jobs" collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "jobs")
public class JobDocument {

    @Id
    private String id;

    private String jobUrl;
    private String jobTitle;
    private String company;
    private String jobDescriptionText;

    @Builder.Default
    private Instant scrapedAt = Instant.now();
}

