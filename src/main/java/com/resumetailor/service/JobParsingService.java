package com.resumetailor.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;

@Slf4j
@Service
public class JobParsingService {

    private static final int TIMEOUT_MS = 30_000;

    private final Playwright playwright;
    private final Browser browser;

    public JobParsingService() {
        log.info("Initializing Playwright with headless Chromium...");
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
        log.info("Playwright browser launched successfully");
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down Playwright browser...");
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Scrapes a job posting URL using a headless Chromium browser (Playwright)
     * to render JavaScript, then parses the rendered HTML with Jsoup.
     *
     * @param jobUrl The URL of the job posting
     * @return Extracted job description text
     * @throws IOException              If the URL is unreachable or blocked
     * @throws IllegalArgumentException If the URL is invalid or empty
     */
    public String scrapeJobDescription(String jobUrl) throws IOException {
        if (jobUrl == null || jobUrl.isBlank()) {
            throw new IllegalArgumentException("Job URL cannot be empty");
        }

        if (!jobUrl.startsWith("http://") && !jobUrl.startsWith("https://")) {
            jobUrl = "https://" + jobUrl;
        }

        log.info("Scraping job description from: {}", jobUrl);

        String renderedHtml;
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
             Page page = context.newPage()) {

            page.setDefaultTimeout(TIMEOUT_MS);

            // Navigate and wait for the DOM content to fully load (JS rendered)
            page.navigate(jobUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(TIMEOUT_MS));

            // Give extra time for any lazy-loaded or async JS content
            page.waitForTimeout(3000);

            renderedHtml = page.content();

        } catch (PlaywrightException e) {
            log.error("Playwright error scraping URL: {}", jobUrl, e);
            throw new IOException("Failed to render the job posting page. " +
                    "The website may be blocking automated access or the URL is invalid.", e);
        }

        // Parse the fully rendered HTML with Jsoup
        Document doc = Jsoup.parse(renderedHtml);

        String jobText = extractFromJobSelectors(doc);

        if (jobText.isEmpty()) {
            jobText = extractCleanBodyText(doc);
        }

        if (jobText.isBlank()) {
            throw new IOException("Could not extract meaningful job description from the provided URL");
        }

        log.info("Successfully scraped {} characters from job posting", jobText.length());
        return jobText.trim();
    }

    /**
     * Attempts to extract job description from common CSS selectors
     * used by popular job boards.
     */
    private String extractFromJobSelectors(Document doc) {
        String[] selectors = {
                "[class*='job-description']",
                "[class*='jobDescription']",
                "[class*='job_description']",
                "[id*='job-description']",
                "[id*='jobDescription']",
                "[class*='description']",
                "[class*='position']",
                "[id*='position']",
                "[class*='responsibility']",
                "[class*='responsibilities']",
                "[id*='responsibility']",
                "[id*='responsibilities']",
                "[class*='experience']",
                "[id*='experience']",
                "[class*='qualification']",
                "[class*='qualifications']",
                "[id*='qualification']",
                "[id*='qualifications']",
                "[class*='requirement']",
                "[class*='requirements']",
                "[id*='requirement']",
                "[id*='requirements']",
                ".job-details",
                ".posting-requirements",
                "[data-testid*='description']",
                "[data-testid*='position']",
                "[data-testid*='responsibilities']",
                "[data-testid*='requirements']",
                "article",
                "[role='main']",
                ".content-body",
                ".job-content"
        };

        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.first().text();
                if (text.length() > 100) {
                    log.debug("Found job description via selector: {}", selector);
                    return text;
                }
            }
        }

        return "";
    }

    /**
     * Fallback: Extract clean body text by removing nav, footer, script, etc.
     */
    private String extractCleanBodyText(Document doc) {
        doc.select("nav, footer, header, script, style, noscript, iframe, " +
                "[role='navigation'], [role='banner'], [role='contentinfo']").remove();

        Element body = doc.body();
        if (body == null) {
            return "";
        }

        String text = body.text();

        if (text.length() > 10_000) {
            text = text.substring(0, 10_000);
        }

        return text;
    }

    /**
     * Extracts a likely job title from the page using Playwright.
     */
    public String extractJobTitle(String jobUrl) {
        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            page.navigate(jobUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(TIMEOUT_MS));

            return page.title();
        } catch (Exception e) {
            log.warn("Could not extract job title from URL: {}", jobUrl);
            return "";
        }
    }
}
