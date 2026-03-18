package com.resumetailor.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class PdfGenerationService {

    private static final DeviceRgb HEADING_COLOR = new DeviceRgb(33, 37, 41);
    private static final DeviceRgb BODY_COLOR = new DeviceRgb(52, 58, 64);
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(0, 102, 204);

    /**
     * Generates a clean, professionally formatted PDF from tailored CV text.
     *
     * @param tailoredCvText The tailored CV content
     * @param candidateName  The candidate's name (used as the title)
     * @return PDF as a byte array
     */
    public byte[] generatePdf(String tailoredCvText, String candidateName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Title: Candidate Name
            if (candidateName != null && !candidateName.isBlank()) {
                Paragraph title = new Paragraph(candidateName)
                        .setFont(boldFont)
                        .setFontSize(22)
                        .setFontColor(HEADING_COLOR)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(5);
                document.add(title);

                // Divider line under the name
                SolidLine line = new SolidLine(1f);
                line.setColor(ACCENT_COLOR);
                document.add(new LineSeparator(line).setMarginBottom(15));
            }

            // Parse and format the CV text
            String[] lines = tailoredCvText.split("\n");
            for (String cvLine : lines) {
                String trimmed = cvLine.trim();

                if (trimmed.isEmpty()) {
                    document.add(new Paragraph("").setMarginBottom(5));
                    continue;
                }

                // Detect section headers (all caps or ending with colon)
                if (isSectionHeader(trimmed)) {
                    // Add spacing before section
                    document.add(new Paragraph("").setMarginBottom(8));

                    Paragraph sectionHeader = new Paragraph(trimmed.replaceAll(":$", ""))
                            .setFont(boldFont)
                            .setFontSize(13)
                            .setFontColor(ACCENT_COLOR)
                            .setMarginBottom(3);
                    document.add(sectionHeader);

                    // Section divider
                    SolidLine sectionLine = new SolidLine(0.5f);
                    sectionLine.setColor(new DeviceRgb(200, 200, 200));
                    document.add(new LineSeparator(sectionLine).setMarginBottom(5));

                } else if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    // Bullet point
                    String bulletText = trimmed.replaceAll("^[•\\-*]\\s*", "");
                    Paragraph bullet = new Paragraph("• " + bulletText)
                            .setFont(regularFont)
                            .setFontSize(10.5f)
                            .setFontColor(BODY_COLOR)
                            .setMarginLeft(15)
                            .setMarginBottom(2);
                    document.add(bullet);

                } else {
                    // Regular paragraph
                    Paragraph paragraph = new Paragraph(trimmed)
                            .setFont(regularFont)
                            .setFontSize(10.5f)
                            .setFontColor(BODY_COLOR)
                            .setMarginBottom(3);
                    document.add(paragraph);
                }
            }

            document.close();
            log.info("PDF generated successfully ({} bytes)", baos.size());

        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage(), e);
            throw new IOException("PDF generation failed: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    /**
     * Heuristic to detect section headers in CV text.
     */
    private boolean isSectionHeader(String line) {
        // All uppercase lines (e.g., "EXPERIENCE", "EDUCATION")
        if (line.equals(line.toUpperCase()) && line.length() > 2 && line.length() < 50
                && line.matches("[A-Z\\s&/]+:?")) {
            return true;
        }
        // Lines ending with colon (e.g., "Experience:", "Skills:")
        if (line.endsWith(":") && line.length() < 40) {
            return true;
        }
        // Common section headers
        String lower = line.toLowerCase().replaceAll("[:\\s]+$", "");
        return lower.matches("(professional\\s+)?(work\\s+)?experience|education|skills|" +
                "technical\\s+skills|certifications?|projects?|summary|objective|" +
                "qualifications|achievements|awards|references|contact|profile");
    }
}
