package com.example.demo.dto;

public class DocumentUploadResponse {
    
    private String message;
    private Long documentId;
    private String documentName;
    private Integer chunksCreated;
    private String status;
    
    public DocumentUploadResponse() {}
    
    public DocumentUploadResponse(String message, Long documentId, String documentName, Integer chunksCreated) {
        this.message = message;
        this.documentId = documentId;
        this.documentName = documentName;
        this.chunksCreated = chunksCreated;
        this.status = "success";
    }
    
    // Static factory method for error responses
    public static DocumentUploadResponse error(String message) {
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.message = message;
        response.status = "error";
        return response;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    
    public String getDocumentName() {
        return documentName;
    }
    
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
    
    public Integer getChunksCreated() {
        return chunksCreated;
    }
    
    public void setChunksCreated(Integer chunksCreated) {
        this.chunksCreated = chunksCreated;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}