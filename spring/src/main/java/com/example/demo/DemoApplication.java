package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main application class for Legal/Policy Compliance Checker
 * 
 * This Spring Boot application provides RAG-based compliance checking using:
 * - Document processing (PDF/DOCX) with Apache PDFBox and POI
 * - Text embedding generation with Vertex AI
 * - Vector similarity search for relevant context retrieval
 * - Compliance answer generation with Gemini LLM
 */
@SpringBootApplication
@EnableConfigurationProperties
public class DemoApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	public static void main(String[] args) {
		logger.info("Starting ComplianceAI...");
		
		try {
			SpringApplication.run(DemoApplication.class, args);
			logger.info("ComplianceAI started successfully on port 9090");
			
		} catch (Exception e) {
			logger.error("Failed to start ComplianceAI: {}", e.getMessage(), e);
			System.exit(1);
		}
	}
}
