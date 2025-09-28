package com.example.demo.config;

import com.example.demo.service.VertexAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

/**
 * Configuration class for Vertex AI integration
 */
@Configuration
public class VertexAIConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(VertexAIConfig.class);
    
    @Value("${google.cloud.project-id}")
    private String projectId;
    
    @Value("${google.cloud.location}")
    private String location;
    
    @Value("${google.cloud.credentials.location:}")
    private String credentialsLocation;
    
    @PostConstruct
    public void init() {
        logger.info("Initializing Vertex AI configuration...");
        logger.info("Project ID: {}", projectId);
        logger.info("Location: {}", location);
        
        if (credentialsLocation.isEmpty()) {
            logger.info("Using default Google Cloud credentials (Application Default Credentials)");
        } else {
            logger.info("Using credentials from: {}", credentialsLocation);
        }
        
        // Validate configuration
        validateConfiguration();
    }
    
    @Bean
    public VertexAIService vertexAIService() {
        logger.info("Creating VertexAIService bean");
        return new VertexAIService();
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down Vertex AI configuration...");
    }
    
    /**
     * Validate Vertex AI configuration
     */
    private void validateConfiguration() {
        if (projectId == null || projectId.trim().isEmpty() || projectId.equals("your-project-id")) {
            logger.warn("Google Cloud Project ID not properly configured. Please set google.cloud.project-id property");
        }
        
        if (location == null || location.trim().isEmpty()) {
            logger.warn("Google Cloud Location not properly configured. Please set google.cloud.location property");
        }
        
        // Check for common configuration issues
        if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null && credentialsLocation.isEmpty()) {
            logger.warn("Google Cloud credentials not found. Please set GOOGLE_APPLICATION_CREDENTIALS environment variable or google.cloud.credentials.location property");
        }
    }
}