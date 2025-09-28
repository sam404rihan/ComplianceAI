package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ComplianceQueryRequest {
    
    @NotBlank(message = "Query is required")
    private String query;
    
    private Integer maxResults = 5; // Number of top chunks to retrieve
    
    public ComplianceQueryRequest() {}
    
    public ComplianceQueryRequest(String query) {
        this.query = query;
    }
    
    public ComplianceQueryRequest(String query, Integer maxResults) {
        this.query = query;
        this.maxResults = maxResults;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public Integer getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
}