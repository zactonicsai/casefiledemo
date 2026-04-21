package gov.fbi.casemgmt.ocr;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * OCR via Tesseract (bundled in the backend Docker image).
 *
 * <ul>
 *   <li>For image content types, runs Tesseract directly on the bytes.</li>
 *   <li>For PDFs, renders each page to an image via PDFBox then OCRs.</li>
 * </ul>
 */
@Service
@Slf4j
public class OcrService {

    @Value("${app.ocr.datapath}")
    private String tessdataPath;

    @Value("${app.ocr.languages:eng}")
    private String languages;

    @Value("${app.ocr.max-pages:500}")
    private int maxPages;

    private ITesseract tesseract;

    @PostConstruct
    void init() {
        tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(languages);
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(1);
    }

    public record OcrResult(String text, int pageCount) {}

    public OcrResult ocr(String contentType, byte[] bytes) {
        if (contentType != null && contentType.startsWith("image/")) {
            return ocrImage(bytes);
        }
        if (contentType != null && contentType.equals("application/pdf")) {
            return ocrPdf(bytes);
        }
        // Unsupported for OCR — return nothing rather than fail.
        return new OcrResult("", 0);
    }

    private OcrResult ocrImage(byte[] bytes) {
        try {
            BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return new OcrResult("", 0);
            String text = tesseract.doOCR(img);
            return new OcrResult(safe(text), 1);
        } catch (IOException | TesseractException e) {
            log.error("OCR failed on image: {}", e.getMessage());
            return new OcrResult("", 0);
        }
    }

    private OcrResult ocrPdf(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int pageCount = 0;
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = Math.min(doc.getNumberOfPages(), maxPages);
            for (int i = 0; i < pages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 220, ImageType.GRAY);
                String pageText = tesseract.doOCR(img);
                sb.append("--- Page ").append(i + 1).append(" ---\n")
                  .append(safe(pageText)).append("\n");
                pageCount++;
            }
        } catch (IOException | TesseractException e) {
            log.error("OCR failed on PDF: {}", e.getMessage());
        }
        return new OcrResult(sb.toString(), pageCount);
    }

    private String safe(String s) {
        return s == null ? "" : s.replaceAll("\\p{C}", " ").strip();
    }
}
