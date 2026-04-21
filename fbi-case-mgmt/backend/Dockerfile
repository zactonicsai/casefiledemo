# ================================================================
# Stage 1 — Build
# ================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ================================================================
# Stage 2 — Runtime
# ================================================================
FROM eclipse-temurin:21-jre-jammy

# Tesseract for OCR + LibreOffice for .docx/.xlsx -> PDF conversion.
# Ghostscript for PDF manipulation. Fonts for headless rendering.
RUN apt-get update && apt-get install -y --no-install-recommends \
      tesseract-ocr \
      tesseract-ocr-eng \
      libreoffice-core \
      libreoffice-writer \
      libreoffice-calc \
      libreoffice-impress \
      ghostscript \
      fonts-liberation \
      fonts-dejavu \
      poppler-utils \
      curl \
      wget \
      ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Run as non-root user.
RUN groupadd -r cms && useradd -r -g cms -d /home/cms -m cms

WORKDIR /app
COPY --from=build /workspace/target/sentinel-backend.jar app.jar
RUN chown -R cms:cms /app

USER cms
EXPOSE 8081

# JVM options for containerized runs.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
