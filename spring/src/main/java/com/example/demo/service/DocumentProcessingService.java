package com.example.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DocumentProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    
    @Value("${document.chunk.size:500}")
    private int chunkSize;
    
    @Value("${document.chunk.overlap:50}")
    private int chunkOverlap;
    
    private static final Pattern SENTENCE_SPLITTER = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern CLAUSE_PATTERN = Pattern.compile("(?i)\\b(?:clause|section|article|paragraph)\\s+[\\d.]+", Pattern.CASE_INSENSITIVE);
    
    /**
     * Extracts text from uploaded file based on file type
     */
    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        
        String fileType = getFileExtension(fileName).toLowerCase();
        
        switch (fileType) {
            case "pdf":
                return extractTextFromPDF(file);
            case "docx":
                return extractTextFromDOCX(file);
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
    
    /**
     * Extracts text from PDF file using Apache PDFBox
     */
    private String extractTextFromPDF(MultipartFile file) throws IOException {
        logger.debug("Extracting text from PDF: {}", file.getOriginalFilename());
        
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            
            logger.debug("Extracted {} characters from PDF", text.length());
            return cleanText(text);
        }
    }
    
    /**
     * Extracts text from DOCX file using Apache POI
     */
    private String extractTextFromDOCX(MultipartFile file) throws IOException {
        logger.debug("Extracting text from DOCX: {}", file.getOriginalFilename());
        
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder textBuilder = new StringBuilder();
            
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                    textBuilder.append(paragraphText).append("\n");
                }
            }
            
            String text = textBuilder.toString();
            logger.debug("Extracted {} characters from DOCX", text.length());
            return cleanText(text);
        }
    }
    
    /**
     * Splits text into chunks with overlapping content for better context preservation
     */
    public List<String> chunkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_SPLITTER.split(text.trim());
        
        if (sentences.length == 0) {
            return List.of(text);
        }
        
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.isEmpty()) continue;
            
            int sentenceTokens = estimateTokenCount(sentence);
            
            // If adding this sentence would exceed chunk size, finalize current chunk
            if (currentTokens + sentenceTokens > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // Create new chunk with overlap from previous chunk
                currentChunk = new StringBuilder();
                currentTokens = 0;
                
                // Add overlap from previous sentences
                int overlapTokens = 0;
                for (int j = Math.max(0, i - 3); j < i && overlapTokens < chunkOverlap; j++) {
                    String overlapSentence = sentences[j].trim();
                    if (!overlapSentence.isEmpty()) {
                        int overlapSentenceTokens = estimateTokenCount(overlapSentence);
                        if (overlapTokens + overlapSentenceTokens <= chunkOverlap) {
                            currentChunk.append(overlapSentence).append(" ");
                            overlapTokens += overlapSentenceTokens;
                        }
                    }
                }
                currentTokens = overlapTokens;
            }
            
            currentChunk.append(sentence).append(" ");
            currentTokens += sentenceTokens;
        }
        
        // Add the final chunk if it has content
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        logger.debug("Split text into {} chunks", chunks.size());
        return chunks;
    }
    
    /**
     * Extracts clause references from text
     */
    public List<String> extractClauseReferences(String text) {
        List<String> clauses = new ArrayList<>();
        var matcher = CLAUSE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String clause = matcher.group().trim();
            if (!clauses.contains(clause)) {
                clauses.add(clause);
            }
        }
        
        return clauses;
    }
    
    /**
     * Estimates token count (rough approximation: 4 characters per token)
     */
    private int estimateTokenCount(String text) {
        return Math.max(1, text.length() / 4);
    }
    
    /**
     * Cleans extracted text by removing extra whitespaces and normalizing
     */
    private String cleanText(String text) {
        if (text == null) return "";
        
        return text
                // Replace multiple whitespaces with single space
                .replaceAll("\\s+", " ")
                // Remove extra newlines but preserve paragraph breaks
                .replaceAll("\\n\\s*\\n", "\n\n")
                // Trim whitespace
                .trim();
    }
    
    /**
     * Gets file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}