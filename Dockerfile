# Multi-stage Dockerfile for Resume Tailor Backend

# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency files first for Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime (Debian-based for Playwright)
# ============================================
FROM ubuntu:22.04

# Install JRE 21, curl, and other essentials
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        wget \
        curl \
        ca-certificates \
        gnupg \
        maven && \
    # Add Eclipse Temurin (Adoptium) repository for JDK 21
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb jammy main" > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends temurin-21-jre && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy pom.xml and install Playwright browser binaries + OS dependencies
COPY pom.xml .
COPY --from=build /root/.m2 /root/.m2
RUN mvn dependency:go-offline -B && \
    mvn exec:java -e \
        -Dexec.mainClass=com.microsoft.playwright.CLI \
        -Dexec.args="install --with-deps" && \
    rm -rf /root/.m2

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SERVER_PORT=8080

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
