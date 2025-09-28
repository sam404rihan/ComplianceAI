package com.example.demo.dto;

import java.util.List;

public class ComplianceQueryResponse {
    
    private String answer;
    private List<String> referencedClauses;
    private Double confidence;
    private String status;
    
    public ComplianceQueryResponse() {}
    
    public ComplianceQueryResponse(String answer, List<String> referencedClauses) {
        this.answer = answer;
        this.referencedClauses = referencedClauses;
        this.status = "success";
    }
    
    public ComplianceQueryResponse(String answer, List<String> referencedClauses, Double confidence) {
        this.answer = answer;
        this.referencedClauses = referencedClauses;
        this.confidence = confidence;
        this.status = "success";
    }
    
    // Static factory method for error responses
    public static ComplianceQueryResponse error(String message) {
        ComplianceQueryResponse response = new ComplianceQueryResponse();
        response.answer = message;
        response.status = "error";
        return response;
    }
    
    // Getters and Setters
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public List<String> getReferencedClauses() {
        return referencedClauses;
    }
    
    public void setReferencedClauses(List<String> referencedClauses) {
        this.referencedClauses = referencedClauses;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}