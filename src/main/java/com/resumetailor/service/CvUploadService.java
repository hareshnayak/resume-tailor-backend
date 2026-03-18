package com.resumetailor.service;

import com.resumetailor.model.*;
import com.resumetailor.repository.CvDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvUploadService {

    private final CvDocumentRepository cvDocumentRepository;
    private final Tika tika = new Tika();

    /**
     * Retrieves a stored CvDocument by userId.
     *
     * @throws IllegalArgumentException if no resume is found for the userId
     */
    public CvDocument findByUserId(String userId) {
        return cvDocumentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No resume found for userId: " + userId));
    }

    /**
     * Extracts text content from an uploaded CV file using Apache Tika.
     * Does not store anything — just returns the raw text.
     */
    public String extractText(MultipartFile file) throws IOException, TikaException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CV file is empty");
        }

        String extractedText;
        try (InputStream inputStream = file.getInputStream()) {
            extractedText = tika.parseToString(inputStream);
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new TikaException("Could not extract any text from the uploaded file");
        }

        log.info("Extracted {} characters from CV file: {}", extractedText.length(), file.getOriginalFilename());
        return extractedText;
    }

    /**
     * Processes an uploaded CV file: extracts text via Tika,
     * parses it into structured fields, and stores in MongoDB.
     *
     * If the user already has a resume on file, the current state is
     * snapshotted into the {@code versions} history list, the version
     * counter is incremented, and the top-level fields are updated
     * with the new upload — so nothing is ever deleted.
     */
    public CvDocument processAndStore(MultipartFile file, String userId) throws IOException, TikaException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CV file is empty");
        }

        // Extract text content using Apache Tika
        String extractedText;
        try (InputStream inputStream = file.getInputStream()) {
            extractedText = tika.parseToString(inputStream);
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new TikaException("Could not extract any text from the uploaded file");
        }

        log.info("Extracted {} characters from CV file: {}", extractedText.length(), file.getOriginalFilename());

        // Parse the extracted text into structured fields
        ContactInfo contactInfo = extractContactInfo(extractedText);
        List<Experience> experience = extractExperience(extractedText);
        List<Education> education = extractEducation(extractedText);
        List<String> skills = extractSkills(extractedText);

        // Check if a document already exists for this user
        var existingOpt = cvDocumentRepository.findByUserId(userId);

        if (existingOpt.isPresent()) {
            // ── Update existing document (version history preserved) ──
            CvDocument existing = existingOpt.get();

            // Snapshot the current state into the versions list
            ResumeVersion snapshot = ResumeVersion.builder()
                    .versionNumber(existing.getCurrentVersion())
                    .originalText(existing.getOriginalText())
                    .contactInfo(existing.getContactInfo())
                    .experience(existing.getExperience())
                    .education(existing.getEducation())
                    .skills(existing.getSkills())
                    .originalFileName(existing.getOriginalFileName())
                    .fileType(existing.getFileType())
                    .uploadedAt(existing.getLastUpdated())
                    .build();
            existing.getVersions().add(snapshot);

            // Increment version and update top-level fields
            existing.setCurrentVersion(existing.getCurrentVersion() + 1);
            existing.setOriginalText(extractedText);
            existing.setContactInfo(contactInfo);
            existing.setExperience(experience);
            existing.setEducation(education);
            existing.setSkills(skills);
            existing.setOriginalFileName(file.getOriginalFilename());
            existing.setFileType(file.getContentType());
            existing.setLastUpdated(Instant.now());
            // tailoredVersions are intentionally kept as-is

            log.info("Updated resume for user {} → version {}", userId, existing.getCurrentVersion());
            return cvDocumentRepository.save(existing);

        } else {
            // ── First upload for this user ──
            CvDocument cvDocument = CvDocument.builder()
                    .userId(userId)
                    .currentVersion(1)
                    .originalText(extractedText)
                    .contactInfo(contactInfo)
                    .experience(experience)
                    .education(education)
                    .skills(skills)
                    .versions(new ArrayList<>())
                    .tailoredVersions(new ArrayList<>())
                    .originalFileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .lastUpdated(Instant.now())
                    .build();

            log.info("Created new resume for user {} → version 1", userId);
            return cvDocumentRepository.save(cvDocument);
        }
    }

    /**
     * Best-effort extraction of contact info from CV text.
     */
    private ContactInfo extractContactInfo(String text) {
        String[] lines = text.split("\n");
        String name = lines.length > 0 ? lines[0].trim() : "";

        // Extract email
        String email = "";
        Matcher emailMatcher = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(text);
        if (emailMatcher.find()) {
            email = emailMatcher.group();
        }

        // Extract phone
        String phone = "";
        Matcher phoneMatcher = Pattern.compile("(\\+?\\d[\\d\\s\\-().]{7,}\\d)").matcher(text);
        if (phoneMatcher.find()) {
            phone = phoneMatcher.group().trim();
        }

        // Extract LinkedIn
        String linkedin = "";
        Matcher linkedinMatcher = Pattern.compile("linkedin\\.com/in/[\\w-]+", Pattern.CASE_INSENSITIVE).matcher(text);
        if (linkedinMatcher.find()) {
            linkedin = linkedinMatcher.group();
        }

        return ContactInfo.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .linkedin(linkedin)
                .build();
    }

    /**
     * Best-effort extraction of work experience entries.
     */
    private List<Experience> extractExperience(String text) {
        List<Experience> experiences = new ArrayList<>();
        // Look for section headers like "Experience", "Work Experience", "Professional Experience"
        Pattern sectionPattern = Pattern.compile(
                "(?i)(?:work\\s+)?(?:professional\\s+)?experience[:\\s]*\\n([\\s\\S]*?)(?=\\n(?:education|skills|certifications|projects|references|$))",
                Pattern.CASE_INSENSITIVE
        );
        Matcher sectionMatcher = sectionPattern.matcher(text);

        if (sectionMatcher.find()) {
            String experienceSection = sectionMatcher.group(1);
            // Split by lines that look like job titles (contain a company or date indicator)
            String[] entries = experienceSection.split("\\n(?=\\S)");
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                String[] entryLines = entry.trim().split("\\n");
                String title = entryLines.length > 0 ? entryLines[0].trim() : "";
                String company = entryLines.length > 1 ? entryLines[1].trim() : "";
                List<String> bullets = new ArrayList<>();
                for (int i = 2; i < entryLines.length; i++) {
                    String line = entryLines[i].trim();
                    if (!line.isEmpty()) {
                        // Remove bullet characters
                        line = line.replaceAll("^[•\\-*]\\s*", "");
                        bullets.add(line);
                    }
                }
                if (!title.isEmpty()) {
                    experiences.add(Experience.builder()
                            .title(title)
                            .company(company)
                            .bulletPoints(bullets)
                            .build());
                }
            }
        }

        return experiences;
    }

    /**
     * Best-effort extraction of education entries.
     */
    private List<Education> extractEducation(String text) {
        List<Education> educations = new ArrayList<>();
        Pattern sectionPattern = Pattern.compile(
                "(?i)education[:\\s]*\\n([\\s\\S]*?)(?=\\n(?:experience|skills|certifications|projects|references|$))",
                Pattern.CASE_INSENSITIVE
        );
        Matcher sectionMatcher = sectionPattern.matcher(text);

        if (sectionMatcher.find()) {
            String educationSection = sectionMatcher.group(1);
            String[] entries = educationSection.split("\\n(?=\\S)");
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                String[] entryLines = entry.trim().split("\\n");
                String degree = entryLines.length > 0 ? entryLines[0].trim() : "";
                String institution = entryLines.length > 1 ? entryLines[1].trim() : "";
                if (!degree.isEmpty()) {
                    educations.add(Education.builder()
                            .degree(degree)
                            .institution(institution)
                            .build());
                }
            }
        }

        return educations;
    }

    /**
     * Best-effort extraction of skills from CV text.
     */
    private List<String> extractSkills(String text) {
        Pattern sectionPattern = Pattern.compile(
                "(?i)(?:technical\\s+)?skills[:\\s]*\\n([\\s\\S]*?)(?=\\n(?:experience|education|certifications|projects|references|$))",
                Pattern.CASE_INSENSITIVE
        );
        Matcher sectionMatcher = sectionPattern.matcher(text);

        if (sectionMatcher.find()) {
            String skillsSection = sectionMatcher.group(1);
            return Arrays.stream(skillsSection.split("[,;\\n•\\-*|]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.length() < 50)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
