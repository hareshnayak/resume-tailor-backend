package com.resumetailor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Programmatic CV auditor — compares extracted job keywords against
 * the CV text and returns those that are MISSING or UNDERREPRESENTED.
 * Replaces the LLM-based CvAuditor with a zero-AI approach.
 *
 * Strategy:
 *   For each keyword, check if it appears in the CV text
 *   (case-insensitive, word-boundary aware).
 *   Keywords not found → "missing".
 */
@Slf4j
@Service
public class CvAuditorService {

    /**
     * Compares job keywords against the CV text and returns the ones
     * that are missing or not mentioned.
     *
     * @param cvText   The full CV text
     * @param keywords The list of keywords extracted from the job description
     * @return List of keywords that do NOT appear in the CV
     */
    public List<String> findMissingKeywords(String cvText, List<String> keywords) {
        if (cvText == null || cvText.isBlank() || keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        String cvLower = cvText.toLowerCase();
        List<String> missing = new ArrayList<>();

        for (String keyword : keywords) {
            if (!isPresent(cvLower, keyword)) {
                missing.add(keyword);
            }
        }

        log.info("CV audit complete: {}/{} keywords missing", missing.size(), keywords.size());
        log.debug("Missing keywords: {}", missing);
        return missing;
    }

    /**
     * Checks if a keyword is present in the CV text.
     * Handles multi-word phrases, abbreviations, and common variants.
     */
    private boolean isPresent(String cvLower, String keyword) {
        String kwLower = keyword.toLowerCase().trim();

        // Direct word-boundary match
        String regex = "\\b" + Pattern.quote(kwLower) + "\\b";
        if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(cvLower).find()) {
            return true;
        }

        // Handle dotted abbreviations — e.g. "CI/CD" also matches "ci cd" or "ci / cd"
        if (kwLower.contains("/")) {
            String relaxed = kwLower.replace("/", "\\s*/\\s*|\\s+");
            if (Pattern.compile(relaxed, Pattern.CASE_INSENSITIVE).matcher(cvLower).find()) {
                return true;
            }
        }

        // Handle ".js" suffixed frameworks — e.g. "React.js" should match "React" and vice-versa
        if (kwLower.endsWith(".js")) {
            String base = kwLower.replace(".js", "");
            if (Pattern.compile("\\b" + Pattern.quote(base) + "(?:\\.js)?\\b",
                    Pattern.CASE_INSENSITIVE).matcher(cvLower).find()) {
                return true;
            }
        }

        // Handle common synonyms
        return checkSynonyms(cvLower, kwLower);
    }

    /**
     * Handles a small set of common synonyms/aliases so that
     * e.g. "K8s" matches "Kubernetes", "JS" matches "JavaScript", etc.
     */
    private boolean checkSynonyms(String cvLower, String kwLower) {
        return switch (kwLower) {
            case "kubernetes", "k8s" ->
                    cvLower.contains("kubernetes") || cvLower.contains("k8s");
            case "javascript", "js" ->
                    cvLower.contains("javascript") || cvLower.contains(" js ");
            case "typescript", "ts" ->
                    cvLower.contains("typescript") || cvLower.contains(" ts ");
            case "golang", "go" ->
                    cvLower.contains("golang") || Pattern.compile("\\bgo\\b").matcher(cvLower).find();
            case "node.js", "nodejs", "node" ->
                    cvLower.contains("node.js") || cvLower.contains("nodejs") || cvLower.contains("node js");
            case "react.js", "reactjs", "react" ->
                    cvLower.contains("react.js") || cvLower.contains("reactjs") || cvLower.contains("react");
            case "vue.js", "vuejs", "vue" ->
                    cvLower.contains("vue.js") || cvLower.contains("vuejs") || cvLower.contains("vue");
            case "aws", "amazon web services" ->
                    cvLower.contains("aws") || cvLower.contains("amazon web services");
            case "gcp", "google cloud" ->
                    cvLower.contains("gcp") || cvLower.contains("google cloud");
            case "ci/cd" ->
                    cvLower.contains("ci/cd") || cvLower.contains("ci cd")
                            || (cvLower.contains("continuous integration") && cvLower.contains("continuous delivery"));
            case "rest", "restful" ->
                    cvLower.contains("rest") || cvLower.contains("restful") || cvLower.contains("rest api");
            case "postgresql", "postgres" ->
                    cvLower.contains("postgresql") || cvLower.contains("postgres");
            case "mongodb", "mongo" ->
                    cvLower.contains("mongodb") || cvLower.contains("mongo");
            default -> false;
        };
    }
}

