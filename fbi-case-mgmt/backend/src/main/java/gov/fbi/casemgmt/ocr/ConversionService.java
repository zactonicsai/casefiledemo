package gov.fbi.casemgmt.ocr;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Converts office document formats to PDF using headless LibreOffice and
 * extracts plain text from PDFs via PDFBox.
 */
@Service
@Slf4j
public class ConversionService {

    private static final Set<String> CONVERTIBLE_TYPES = Set.of(
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/rtf",
        "text/rtf",
        "text/plain",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.spreadsheet"
    );

    @Value("${app.conversion.libreoffice-path:/usr/bin/libreoffice}")
    private String libreofficePath;

    @Value("${app.conversion.temp-dir:/tmp/cms-conversion}")
    private String tempRoot;

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(Path.of(tempRoot));
    }

    public boolean isConvertible(String contentType) {
        return contentType != null && CONVERTIBLE_TYPES.contains(contentType);
    }

    /**
     * Converts the given bytes to PDF. Returns the PDF bytes.
     * Throws {@link ConversionException} on failure or timeout.
     */
    public byte[] toPdf(String originalFilename, byte[] bytes) {
        Path workDir = null;
        try {
            workDir = Files.createDirectory(
                Path.of(tempRoot, "job-" + UUID.randomUUID()));

            String inName = originalFilename != null ? sanitizeName(originalFilename) : "input.bin";
            Path in = workDir.resolve(inName);
            Files.write(in, bytes);

            ProcessBuilder pb = new ProcessBuilder(List.of(
                libreofficePath,
                "--headless",
                "--norestore",
                "--nolockcheck",
                "--convert-to", "pdf",
                "--outdir", workDir.toString(),
                in.toString()
            ));
            pb.redirectErrorStream(true);
            pb.environment().put("HOME", workDir.toString());
            Process p = pb.start();

            if (!p.waitFor(120, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new ConversionException("LibreOffice conversion timed out");
            }
            if (p.exitValue() != 0) {
                String out = new String(p.getInputStream().readAllBytes());
                throw new ConversionException("LibreOffice exit " + p.exitValue() + ": " + out);
            }

            String base = stripExtension(inName);
            Path outPdf = workDir.resolve(base + ".pdf");
            if (!Files.exists(outPdf)) {
                throw new ConversionException("Expected output not found: " + outPdf);
            }
            return Files.readAllBytes(outPdf);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ConversionException("Conversion failed: " + e.getMessage(), e);
        } finally {
            if (workDir != null) deleteRecursive(workDir);
        }
    }

    /** Extracts plain text from a PDF using PDFBox. */
    public String extractPdfText(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (IOException e) {
            log.warn("PDF text extraction failed: {}", e.getMessage());
            return "";
        }
    }

    private static String sanitizeName(String n) {
        return n.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String stripExtension(String n) {
        int dot = n.lastIndexOf('.');
        return dot < 0 ? n : n.substring(0, dot);
    }

    private static void deleteRecursive(Path p) {
        try {
            if (!Files.exists(p)) return;
            try (var walk = Files.walk(p)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(f -> { try { Files.deleteIfExists(f); } catch (IOException ignored) {} });
            }
        } catch (IOException ignored) {}
    }

    public static class ConversionException extends RuntimeException {
        public ConversionException(String m) { super(m); }
        public ConversionException(String m, Throwable t) { super(m, t); }
    }
}
