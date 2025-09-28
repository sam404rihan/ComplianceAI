package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Main application configuration
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {
    
    /**
     * Configure CORS for frontend integration
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

/**
 * Document processing configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "document")
class DocumentConfig {
    
    private int chunkSize = 500;
    private int chunkOverlap = 50;
    private String maxFileSize = "10MB";
    
    // Getters and Setters
    public int getChunkSize() {
        return chunkSize;
    }
    
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public int getChunkOverlap() {
        return chunkOverlap;
    }
    
    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }
    
    public String getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}

/**
 * Vertex AI configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "vertex.ai")
class VertexAIProperties {
    
    private Model model = new Model();
    private int maxTokens = 8192;
    private double temperature = 0.2;
    
    public static class Model {
        private String embedding = "text-embedding-004";
        private String generation = "gemini-1.5-flash";
        
        // Getters and Setters
        public String getEmbedding() {
            return embedding;
        }
        
        public void setEmbedding(String embedding) {
            this.embedding = embedding;
        }
        
        public String getGeneration() {
            return generation;
        }
        
        public void setGeneration(String generation) {
            this.generation = generation;
        }
    }
    
    // Getters and Setters
    public Model getModel() {
        return model;
    }
    
    public void setModel(Model model) {
        this.model = model;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}