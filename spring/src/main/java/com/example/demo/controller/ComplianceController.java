package com.example.demo.controller;

import com.example.demo.dto.ComplianceQueryRequest;
import com.example.demo.dto.ComplianceQueryResponse;
import com.example.demo.dto.DocumentUploadResponse;
import com.example.demo.service.DocumentProcessingService;
import com.example.demo.service.VectorStoreService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {
    
    private static final Logger logger = LoggerFactory.getLogger(ComplianceController.class);
    
    @Autowired
    private DocumentProcessingService documentProcessingService;
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Value("${document.max-file-size:10MB}")
    private String maxFileSize;
    
    @Value("${openai.model.generation:gpt-4o-mini}")
    private String generationModel;
    
    private static final List<String> SUPPORTED_FILE_TYPES = List.of("pdf", "docx");
    
    /**
     * Upload policy document endpoint (PDF/DOCX)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        logger.info("Received document upload request: {}", file.getOriginalFilename());
        
        try {
            // Validate file
            validateFile(file);
            
            // Extract text from document
            String extractedText = documentProcessingService.extractText(file);
            
            if (extractedText.trim().isEmpty()) {
                logger.warn("Extracted text is empty for file: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest()
                        .body(DocumentUploadResponse.error("No text content could be extracted from the document"));
            }
            
            // Split text into chunks
            List<String> chunks = documentProcessingService.chunkText(extractedText);
            
            if (chunks.isEmpty()) {
                logger.warn("No chunks created for file: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest()
                        .body(DocumentUploadResponse.error("Document could not be processed into searchable chunks"));
            }
            
            // Generate a document ID (in real implementation, this would come from database)
            Long documentId = System.currentTimeMillis();
            
            // Store chunks with embeddings in vector store
            List<String> failedChunks = vectorStoreService.storeDocumentChunks(documentId, file.getOriginalFilename(), chunks);
            
            int successfulChunks = chunks.size() - failedChunks.size();
            
            if (successfulChunks == 0) {
                logger.error("All chunks failed to process for document: {}", file.getOriginalFilename());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(DocumentUploadResponse.error("Failed to process document chunks"));
            }
            
            if (!failedChunks.isEmpty()) {
                logger.warn("{} out of {} chunks failed to process for document: {}", 
                           failedChunks.size(), chunks.size(), file.getOriginalFilename());
            }
            
            String message = String.format("Document uploaded and processed successfully. %d chunks created.", successfulChunks);
            if (!failedChunks.isEmpty()) {
                message += String.format(" %d chunks failed to process.", failedChunks.size());
            }
            
            DocumentUploadResponse response = new DocumentUploadResponse(
                    message,
                    documentId,
                    file.getOriginalFilename(),
                    successfulChunks
            );
            
            logger.info("Successfully uploaded and processed document: {} with {} chunks", 
                       file.getOriginalFilename(), successfulChunks);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file upload request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(DocumentUploadResponse.error(e.getMessage()));
            
        } catch (IOException e) {
            logger.error("Error processing document {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentUploadResponse.error("Failed to process document: " + e.getMessage()));
            
        } catch (Exception e) {
            logger.error("Unexpected error uploading document {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentUploadResponse.error("An unexpected error occurred while processing the document"));
        }
    }
    
    /**
     * Query compliance information endpoint
     */
    @PostMapping("/query")
    public ResponseEntity<ComplianceQueryResponse> queryCompliance(@Valid @RequestBody ComplianceQueryRequest request) {
        logger.info("Received compliance query: {}", request.getQuery());
        
        try {
            if (request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ComplianceQueryResponse.error("Query cannot be empty"));
            }
            
            // Set default max results if not provided
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 5;
            
            // Validate max results
            if (maxResults <= 0 || maxResults > 20) {
                return ResponseEntity.badRequest()
                        .body(ComplianceQueryResponse.error("Max results must be between 1 and 20"));
            }
            
            // Search for compliance information
            ComplianceQueryResponse response = vectorStoreService.searchCompliance(request.getQuery(), maxResults);
            
            if ("error".equals(response.getStatus())) {
                logger.warn("Compliance query failed: {}", response.getAnswer());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            logger.info("Successfully processed compliance query with {} referenced clauses", 
                       response.getReferencedClauses() != null ? response.getReferencedClauses().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing compliance query '{}': {}", request.getQuery(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComplianceQueryResponse.error("Failed to process compliance query: " + e.getMessage()));
        }
    }
    
    /**
     * Get vector store statistics (for monitoring/debugging)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = vectorStoreService.getVectorStoreStats();
            logger.debug("Retrieved vector store stats: {}", stats);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve statistics"));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        try {
            Map<String, Object> stats = vectorStoreService.getVectorStoreStats();
            
            return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "service", "Legal Compliance Checker",
                    "documentsProcessed", stats.get("totalDocuments").toString(),
                    "chunksStored", stats.get("totalChunks").toString()
            ));
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "status", "unhealthy",
                            "error", e.getMessage()
                    ));
        }
    }
    
    /**
     * Clear all documents (for testing purposes)
     */
    @DeleteMapping("/documents")
    public ResponseEntity<Map<String, String>> clearAllDocuments() {
        try {
            vectorStoreService.clearAll();
            logger.info("Cleared all documents from vector store");
            return ResponseEntity.ok(Map.of("message", "All documents cleared successfully"));
        } catch (Exception e) {
            logger.error("Error clearing documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear documents"));
        }
    }
    
    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) throws IllegalArgumentException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        
        String fileExtension = getFileExtension(fileName).toLowerCase();
        if (!SUPPORTED_FILE_TYPES.contains(fileExtension)) {
            throw new IllegalArgumentException("Unsupported file type. Supported types: " + SUPPORTED_FILE_TYPES);
        }
        
        // Check file size (Spring Boot handles this automatically via configuration, but we can add custom logic)
        long fileSizeInBytes = file.getSize();
        if (fileSizeInBytes == 0) {
            throw new IllegalArgumentException("File appears to be empty");
        }
        
        // Additional size check (10MB = 10 * 1024 * 1024 bytes)
        long maxSizeInBytes = 10 * 1024 * 1024;
        if (fileSizeInBytes > maxSizeInBytes) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }
    }
    
    /**
     * General chat endpoint using Gemini 2.5 Flash (not compliance-focused)
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        logger.info("Received chat request");
        
        try {
            String message = request.get("message");
            
            if (message == null || message.trim().isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                    "response", "Message cannot be empty",
                    "status", "error"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Use the VectorStoreService to generate a general AI response
            // This will automatically use Gemini 2.5 Flash when no documents are available
            ComplianceQueryResponse aiResponse = vectorStoreService.searchCompliance(message, 1);
            
            Map<String, Object> response = Map.of(
                "response", aiResponse.getAnswer(),
                "status", "success",
                "model", generationModel
            );
            
            logger.info("Chat response generated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing chat request: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "response", "Sorry, I encountered an error processing your message. Please try again.",
                "status", "error"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}