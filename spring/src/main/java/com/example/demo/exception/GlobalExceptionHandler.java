package com.example.demo.exception;

import com.example.demo.dto.ComplianceQueryResponse;
import com.example.demo.dto.DocumentUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Legal Compliance Checker application
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ComplianceQueryResponse> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errorMessage.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; ");
        }
        
        return ResponseEntity.badRequest()
                .body(ComplianceQueryResponse.error(errorMessage.toString()));
    }
    
    /**
     * Handle file upload size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<DocumentUploadResponse> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(DocumentUploadResponse.error("File size exceeds maximum allowed size of 10MB"));
    }
    
    /**
     * Handle missing file in upload request
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<DocumentUploadResponse> handleMissingFileException(MissingServletRequestPartException ex) {
        logger.warn("Missing file in upload request: {}", ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(DocumentUploadResponse.error("File is required for upload"));
    }
    
    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParameterException(MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "Missing required parameter: " + ex.getParameterName());
        error.put("parameter", ex.getParameterName());
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle JSON parsing errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ComplianceQueryResponse> handleJsonParsingException(HttpMessageNotReadableException ex) {
        logger.warn("JSON parsing error: {}", ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(ComplianceQueryResponse.error("Invalid JSON format in request body"));
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        // Return different response types based on context
        if (ex.getMessage().contains("file") || ex.getMessage().contains("upload")) {
            return ResponseEntity.badRequest()
                    .body(DocumentUploadResponse.error(ex.getMessage()));
        } else {
            return ResponseEntity.badRequest()
                    .body(ComplianceQueryResponse.error(ex.getMessage()));
        }
    }
    
    /**
     * Handle IO exceptions (file processing, network issues)
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Object> handleIOException(IOException ex, WebRequest request) {
        logger.error("IO Exception: {}", ex.getMessage(), ex);
        
        String path = request.getDescription(false);
        
        // Return different response types based on endpoint
        if (path.contains("/upload")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentUploadResponse.error("Failed to process document: " + ex.getMessage()));
        } else if (path.contains("/query")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComplianceQueryResponse.error("Failed to process query: " + ex.getMessage()));
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "IO operation failed");
            error.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Handle Google Cloud / Vertex AI specific exceptions
     */
    @ExceptionHandler(com.google.api.gax.rpc.ApiException.class)
    public ResponseEntity<Object> handleVertexAIException(com.google.api.gax.rpc.ApiException ex, WebRequest request) {
        logger.error("Vertex AI API Exception: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
        
        String errorMessage = "AI service temporarily unavailable";
        
        // Provide more specific error messages based on status code
        switch (ex.getStatusCode().getCode()) {
            case UNAUTHENTICATED:
                errorMessage = "Authentication failed with AI service";
                break;
            case PERMISSION_DENIED:
                errorMessage = "Permission denied for AI service";
                break;
            case RESOURCE_EXHAUSTED:
                errorMessage = "AI service quota exceeded";
                break;
            case UNAVAILABLE:
                errorMessage = "AI service is temporarily unavailable";
                break;
            case DEADLINE_EXCEEDED:
                errorMessage = "AI service request timed out";
                break;
            default:
                errorMessage = "AI service error: " + ex.getMessage();
        }
        
        String path = request.getDescription(false);
        if (path.contains("/query")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ComplianceQueryResponse.error(errorMessage));
        } else if (path.contains("/upload")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(DocumentUploadResponse.error(errorMessage));
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", errorMessage);
            error.put("statusCode", ex.getStatusCode().getCode().toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }
    }
    
    /**
     * Handle all other runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Runtime Exception: {}", ex.getMessage(), ex);
        
        String path = request.getDescription(false);
        String errorMessage = "An unexpected error occurred";
        
        // Return different response types based on endpoint
        if (path.contains("/upload")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentUploadResponse.error(errorMessage));
        } else if (path.contains("/query")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComplianceQueryResponse.error(errorMessage));
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", errorMessage);
            error.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Unexpected Exception: {}", ex.getMessage(), ex);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred");
        error.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}