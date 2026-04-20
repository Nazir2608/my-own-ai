package com.nazir.myownai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FileParserService {

    private final Tika tika = new Tika();

    /**
     * Parse uploaded file and extract text content
     * Supports: PDF, DOC, DOCX, TXT
     */
    public String parseFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filename is null");
        }
        String extension = getFileExtension(filename).toLowerCase();

        return switch (extension) {
            case "pdf" -> parsePDF(file);
            case "doc" -> parseDOC(file);
            case "docx" -> parseDOCX(file);
            case "txt" -> parseTXT(file);
            default -> parseWithTika(file);  // Fallback to Apache Tika
        };
    }

    /**
     * Parse PDF using Apache PDFBox
     */
    private String parsePDF(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return cleanText(text);

        } catch (Exception e) {
            throw new IOException("Failed to parse PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Parse old DOC format using Apache POI
     */
    private String parseDOC(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {

            String text = extractor.getText();
            return cleanText(text);
        } catch (Exception e) {
            throw new IOException("Failed to parse DOC: " + e.getMessage(), e);
        }
    }

    /**
     * Parse DOCX format using Apache POI
     */
    private String parseDOCX(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            return cleanText(text);
        } catch (Exception e) {
            throw new IOException("Failed to parse DOCX: " + e.getMessage(), e);
        }
    }

    /**
     * Parse plain text file
     */
    private String parseTXT(MultipartFile file) throws IOException {
        try {
            byte[] bytes = file.getBytes();
            String text = new String(bytes, StandardCharsets.UTF_8);
            return cleanText(text);
        } catch (Exception e) {
            throw new IOException("Failed to parse TXT: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback parser using Apache Tika (supports many formats)
     */
    private String parseWithTika(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            String text = tika.parseToString(inputStream);
            return cleanText(text);
        } catch (Exception e) {
            throw new IOException("Failed to parse file with Tika: " + e.getMessage(), e);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * Clean extracted text
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text
                // Remove excessive whitespace
                .replaceAll("[ \\t]+", " ")
                // Remove excessive newlines (more than 2 consecutive)
                .replaceAll("\\n{3,}", "\n\n")
                // Trim
                .trim();
    }

    /**
     * Get supported file extensions
     */
    public String[] getSupportedExtensions() {
        return new String[]{"pdf", "doc", "docx", "txt"};
    }

    /**
     * Check if file extension is supported
     */
    public boolean isSupportedFile(String filename) {
        if (filename == null) {
            return false;
        }
        String extension = getFileExtension(filename).toLowerCase();
        for (String supported : getSupportedExtensions()) {
            if (supported.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
