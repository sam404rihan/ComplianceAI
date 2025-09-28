package com.example.demo.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAIService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.model.generation:gpt-4o-mini}")
    private String model;
    
    @Value("${openai.max-tokens:4096}")
    private Integer maxTokens;
    
    @Value("${openai.temperature:0.1}")
    private Double temperature;
    
    private OpenAiService openAiService;
    
    @PostConstruct
    public void initializeClient() {
        if (apiKey == null || apiKey.equals("your-openai-api-key-here") || apiKey.trim().isEmpty()) {
            logger.warn("OpenAI API key not configured. Chat generation will use fallback responses.");
            return;
        }
        
        try {
            this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
            logger.info("OpenAI service initialized successfully with model: {}", model);
        } catch (Exception e) {
            logger.error("Failed to initialize OpenAI service", e);
        }
    }
    
    public String generateComplianceAnswer(String query, List<String> relevantChunks) {
        if (openAiService == null) {
            logger.warn("OpenAI service not available. Using fallback response.");
            return "Service unavailable. Please try again later.";
        }
        
        try {
            String prompt = buildCompliancePrompt(query, relevantChunks);
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are a legal compliance expert. Provide accurate, professional responses based on the provided policy documents. Keep responses concise and to the point. Focus on the key compliance points without unnecessary details."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            String response = result.getChoices().get(0).getMessage().getContent();
            
            logger.info("Generated compliance answer successfully using OpenAI");
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating compliance answer with OpenAI", e);
            return "Service unavailable. Please try again later.";
        }
    }
    
    public String generateGeneralAnswer(String query) {
        if (openAiService == null) {
            logger.warn("OpenAI service not available. Using fallback response.");
            return "Hello! Service is currently unavailable. Please try again later.";
        }
        
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are a helpful AI assistant. Provide clear, concise, and helpful responses. Keep answers brief and to the point. Don't over-explain or provide excessive details unless specifically asked."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), query));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            String response = result.getChoices().get(0).getMessage().getContent();
            
            logger.info("Generated general answer successfully using OpenAI");
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating general answer with OpenAI", e);
            return "Hello! Service is currently unavailable. Please try again later.";
        }
    }
    
    private String buildCompliancePrompt(String query, List<String> relevantChunks) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Based on the following policy documents, provide a brief compliance answer:\n\n");
        
        prompt.append("POLICY CONTEXT:\n");
        for (int i = 0; i < relevantChunks.size(); i++) {
            prompt.append(String.format("Document %d:\n%s\n\n", i + 1, relevantChunks.get(i)));
        }
        
        prompt.append("QUESTION: ").append(query).append("\n\n");
        
        prompt.append("Provide a concise, professional response. Focus on key compliance points only. If the answer cannot be determined from the context, state that briefly.");
        
        return prompt.toString();
    }
}