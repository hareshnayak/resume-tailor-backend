package com.resumetailor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Programmatic keyword extraction from job descriptions.
 * Replaces the LLM-based KeywordExtractor with a dictionary +
 * pattern-matching approach — zero AI calls required.
 *
 * Strategy:
 *   1. Match against a curated dictionary of ~300 technical skills,
 *      tools, soft skills, certifications, and methodologies.
 *   2. Extract additional "technical-looking" terms that are capitalised
 *      or follow common patterns (e.g. 3+ years, CI/CD).
 *   3. De-duplicate and return as a List<String>.
 */
@Slf4j
@Service
public class KeywordExtractorService {

    // ── Curated skills dictionary ──────────────────────────────────────────

    private static final Set<String> SKILL_DICTIONARY = new LinkedHashSet<>();

    static {
        // Programming languages
        addAll("Java", "Python", "JavaScript", "TypeScript", "C", "C++", "C#", "Go", "Golang",
                "Rust", "Kotlin", "Scala", "Ruby", "PHP", "Swift", "Objective-C", "R", "MATLAB",
                "Perl", "Lua", "Dart", "Groovy", "Elixir", "Erlang", "Haskell", "Clojure", "SQL",
                "PL/SQL", "T-SQL", "Bash", "Shell", "PowerShell", "HTML", "CSS", "SASS", "LESS");

        // Frameworks & libraries
        addAll("Spring", "Spring Boot", "Spring Cloud", "Spring Security", "Spring Data",
                "Spring MVC", "Spring Batch", "Hibernate", "JPA", "MyBatis",
                "React", "React.js", "Angular", "Vue", "Vue.js", "Next.js", "Nuxt.js",
                "Node.js", "Express", "Express.js", "NestJS", "Django", "Flask", "FastAPI",
                "Rails", "Ruby on Rails", "ASP.NET", ".NET", ".NET Core",
                "Laravel", "Symfony", "Gin", "Fiber", "Echo",
                "TensorFlow", "PyTorch", "scikit-learn", "Pandas", "NumPy",
                "jQuery", "Bootstrap", "Tailwind", "Tailwind CSS", "Material UI",
                "Redux", "MobX", "GraphQL", "gRPC", "Thrift",
                "Quarkus", "Micronaut", "Vert.x", "Akka", "Play Framework",
                "LangChain", "LangChain4j", "Playwright", "Selenium", "Cypress");

        // Databases & data
        addAll("MySQL", "PostgreSQL", "Oracle", "SQL Server", "SQLite",
                "MongoDB", "DynamoDB", "Cassandra", "CouchDB", "Couchbase",
                "Redis", "Memcached", "Elasticsearch", "OpenSearch", "Solr",
                "Neo4j", "InfluxDB", "TimescaleDB",
                "Kafka", "RabbitMQ", "ActiveMQ", "SQS", "SNS", "Kinesis",
                "Snowflake", "Redshift", "BigQuery", "Databricks", "Spark",
                "Hadoop", "Hive", "Flink", "Airflow", "dbt");

        // Cloud & DevOps
        addAll("AWS", "Amazon Web Services", "Azure", "GCP", "Google Cloud",
                "EC2", "S3", "Lambda", "ECS", "EKS", "Fargate", "CloudFormation",
                "API Gateway", "IAM", "VPC", "Route 53", "CloudWatch", "CloudFront",
                "Docker", "Kubernetes", "K8s", "Helm", "Terraform", "Ansible",
                "Puppet", "Chef", "Vagrant", "Packer",
                "Jenkins", "GitHub Actions", "GitLab CI", "CircleCI", "Travis CI",
                "ArgoCD", "Spinnaker", "Bamboo", "TeamCity",
                "Prometheus", "Grafana", "Datadog", "New Relic", "Splunk", "ELK",
                "Nginx", "Apache", "Tomcat", "Istio", "Envoy", "Consul",
                "Vault", "SonarQube", "Nexus", "Artifactory");

        // Methodologies & practices
        addAll("Agile", "Scrum", "Kanban", "SAFe", "Lean",
                "CI/CD", "DevOps", "DevSecOps", "SRE", "GitOps",
                "TDD", "BDD", "DDD", "Event-Driven", "Microservices",
                "Monolith", "Serverless", "REST", "RESTful", "SOAP",
                "OAuth", "OAuth2", "JWT", "SAML", "SSO", "OIDC",
                "MVC", "MVVM", "CQRS", "Event Sourcing",
                "Design Patterns", "SOLID", "Clean Architecture",
                "12-Factor", "API Design", "System Design");

        // Certifications
        addAll("AWS Certified", "Azure Certified", "GCP Certified",
                "CKA", "CKAD", "CKS", "PMP", "TOGAF",
                "Certified Scrum Master", "CSM", "CISSP", "CompTIA");

        // Soft skills & general
        addAll("Leadership", "Communication", "Problem Solving", "Critical Thinking",
                "Teamwork", "Collaboration", "Mentoring", "Coaching",
                "Time Management", "Project Management", "Stakeholder Management",
                "Cross-functional", "Self-motivated", "Detail-oriented",
                "Analytical", "Strategic Thinking", "Decision Making",
                "Presentation", "Public Speaking", "Technical Writing",
                "Client Facing", "Customer Focus");

        // Testing & QA
        addAll("JUnit", "TestNG", "Mockito", "Jest", "Mocha", "Chai",
                "Pytest", "RSpec", "Cucumber", "Selenium", "Appium",
                "JMeter", "Gatling", "Postman", "SoapUI",
                "Unit Testing", "Integration Testing", "E2E Testing",
                "Load Testing", "Performance Testing", "Security Testing");

        // Data / ML / AI
        addAll("Machine Learning", "Deep Learning", "NLP",
                "Natural Language Processing", "Computer Vision",
                "Data Science", "Data Engineering", "Data Pipeline",
                "ETL", "ELT", "Data Warehousing", "Data Lake",
                "Feature Engineering", "Model Training", "MLOps",
                "LLM", "Large Language Model", "Generative AI", "RAG",
                "Vector Database", "Embeddings", "Fine-tuning", "Prompt Engineering");

        // Version control & tools
        addAll("Git", "GitHub", "GitLab", "Bitbucket", "SVN",
                "Jira", "Confluence", "Trello", "Asana", "Notion",
                "Figma", "Sketch", "InVision",
                "VS Code", "IntelliJ", "Eclipse",
                "Maven", "Gradle", "npm", "Yarn", "pnpm", "pip", "Poetry",
                "Webpack", "Vite", "Rollup", "Babel", "ESLint", "Prettier");

        // Mobile
        addAll("Android", "iOS", "React Native", "Flutter", "Xamarin",
                "SwiftUI", "Jetpack Compose", "Kotlin Multiplatform");

        // Security
        addAll("OWASP", "Penetration Testing", "Vulnerability Assessment",
                "Encryption", "TLS", "SSL", "PKI", "Firewall",
                "WAF", "DDoS", "Zero Trust");
    }

    /** Build a lowercased lookup set + keep originals for matching. */
    private static final Map<String, String> LOWERCASE_TO_ORIGINAL = new HashMap<>();

    static {
        for (String skill : SKILL_DICTIONARY) {
            LOWERCASE_TO_ORIGINAL.put(skill.toLowerCase(), skill);
        }
    }

    // ── Patterns for additional extraction ─────────────────────────────────

    /** Matches "N+ years" experience requirements */
    private static final Pattern YEARS_PATTERN =
            Pattern.compile("(\\d+\\+?)\\s*(?:years?|yrs?)\\s+(?:of\\s+)?(?:experience|exp)\\b",
                    Pattern.CASE_INSENSITIVE);

    /** Matches degree requirements */
    private static final Pattern DEGREE_PATTERN =
            Pattern.compile("\\b(Bachelor'?s?|Master'?s?|PhD|Doctorate|B\\.?S\\.?|M\\.?S\\.?|B\\.?E\\.?|M\\.?E\\.?|MBA)\\b" +
                            "(?:\\s+(?:degree|in)\\s+([A-Z][a-zA-Z\\s,]+?)(?=[.;\\n]))?",
                    Pattern.CASE_INSENSITIVE);

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Extracts keywords from a job description using dictionary matching
     * and pattern recognition — no AI model required.
     *
     * @param jobText The raw job description text
     * @return Ordered list of matched keywords (most relevant first)
     */
    public List<String> extractKeywords(String jobText) {
        if (jobText == null || jobText.isBlank()) {
            return List.of();
        }

        String textLower = jobText.toLowerCase();
        Set<String> found = new LinkedHashSet<>();

        // 1. Multi-word matches first (longer phrases before single words)
        List<Map.Entry<String, String>> sorted = LOWERCASE_TO_ORIGINAL.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                .toList();

        for (Map.Entry<String, String> entry : sorted) {
            String lower = entry.getKey();
            String original = entry.getValue();
            // Word-boundary check to avoid partial matches (e.g. "go" inside "google")
            String regex = "\\b" + Pattern.quote(lower) + "\\b";
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(textLower).find()) {
                found.add(original);
            }
        }

        // 2. Extract experience-year requirements
        Matcher yearsMatcher = YEARS_PATTERN.matcher(jobText);
        while (yearsMatcher.find()) {
            found.add(yearsMatcher.group().trim());
        }

        // 3. Extract degree requirements
        Matcher degreeMatcher = DEGREE_PATTERN.matcher(jobText);
        while (degreeMatcher.find()) {
            found.add(degreeMatcher.group().trim());
        }

        log.info("Extracted {} keywords from job description (dictionary + patterns)", found.size());
        log.debug("Keywords: {}", found);
        return new ArrayList<>(found);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void addAll(String... skills) {
        Collections.addAll(SKILL_DICTIONARY, skills);
    }
}


