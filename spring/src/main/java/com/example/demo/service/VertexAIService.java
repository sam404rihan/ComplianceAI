package com.example.demo.service;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.aiplatform.v1.*;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class VertexAIService {
    
    private static final Logger logger = LoggerFactory.getLogger(VertexAIService.class);
    
    @Value("${google.cloud.project-id}")
    private String projectId;
    
    @Value("${google.cloud.location}")
    private String location;
    
    @Value("${vertex.ai.model.embedding:text-embedding-004}")
    private String embeddingModel;
    
    @Value("${vertex.ai.model.generation:gemini-1.5-flash}")
    private String generationModel;
    
    @Value("${vertex.ai.max-tokens:8192}")
    private int maxTokens;
    
    @Value("${vertex.ai.temperature:0.2}")
    private double temperature;
    
    @Value("${vertex.ai.top-p:0.8}")
    private double topP;
    
    @Value("${vertex.ai.top-k:40}")
    private int topK;
    
    @Value("${google.cloud.credentials.location:}")
    private String credentialsPath;
    
    private PredictionServiceClient predictionServiceClient;
    
    /**
     * Initialize the Vertex AI client with proper credentials
     */
    public void initializeClient() throws IOException {
        if (predictionServiceClient == null) {
            try {
                PredictionServiceSettings.Builder settingsBuilder = PredictionServiceSettings.newBuilder();
                
                // Set up credentials if path is provided
                if (!credentialsPath.isEmpty()) {
                    logger.info("Loading credentials from: {}", credentialsPath);
                    ServiceAccountCredentials credentials = ServiceAccountCredentials
                            .fromStream(new FileInputStream(credentialsPath));
                    settingsBuilder.setCredentialsProvider(() -> credentials);
                } else {
                    logger.info("Using Application Default Credentials (ADC)");
                    // Check if GOOGLE_APPLICATION_CREDENTIALS environment variable is set
                    String envCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                    if (envCredentials != null && !envCredentials.isEmpty()) {
                        logger.info("GOOGLE_APPLICATION_CREDENTIALS found: {}", envCredentials);
                    } else {
                        logger.warn("GOOGLE_APPLICATION_CREDENTIALS not set. Falling back to default credentials.");
                    }
                }
                
                predictionServiceClient = PredictionServiceClient.create(settingsBuilder.build());
                logger.info("Vertex AI client initialized successfully for project: {}, location: {}", projectId, location);
                
            } catch (Exception e) {
                logger.error("Failed to initialize Vertex AI client: {}", e.getMessage());
                logger.error("Please ensure Google Cloud credentials are properly configured");
                throw new IOException("Failed to initialize Vertex AI client", e);
            }
        }
    }
    
    /**
     * Generate embeddings for text using Vertex AI Text Embeddings API
     */
    public List<Double> generateEmbeddings(String text) throws IOException {
        initializeClient();
        
        try {
            // Create the endpoint name for the embedding model
            String endpoint = String.format("projects/%s/locations/%s/publishers/google/models/%s", 
                    projectId, location, embeddingModel);
            EndpointName endpointName = EndpointName.parse(endpoint);
            
            // Create the instance for embedding
            Struct.Builder instanceBuilder = Struct.newBuilder();
            instanceBuilder.putFields("content", com.google.protobuf.Value.newBuilder().setStringValue(text).build());
            
            // Set task type for better embeddings
            instanceBuilder.putFields("task_type", com.google.protobuf.Value.newBuilder().setStringValue("RETRIEVAL_DOCUMENT").build());
            
            com.google.protobuf.Value instance = com.google.protobuf.Value.newBuilder().setStructValue(instanceBuilder.build()).build();
            
            // Create parameters (optional for embeddings)
            Struct.Builder parametersBuilder = Struct.newBuilder();
            com.google.protobuf.Value parameters = com.google.protobuf.Value.newBuilder().setStructValue(parametersBuilder.build()).build();
            
            // Make the prediction request
            PredictRequest request = PredictRequest.newBuilder()
                    .setEndpoint(endpointName.toString())
                    .addInstances(instance)
                    .setParameters(parameters)
                    .build();
            
            PredictResponse response = predictionServiceClient.predict(request);
            
            // Extract embeddings from response
            if (!response.getPredictionsList().isEmpty()) {
                com.google.protobuf.Value prediction = response.getPredictionsList().get(0);
                Struct predictionStruct = prediction.getStructValue();
                
                if (predictionStruct.containsFields("embeddings")) {
                    Struct embeddingsStruct = predictionStruct.getFieldsMap().get("embeddings").getStructValue();
                    if (embeddingsStruct.containsFields("values")) {
                        List<com.google.protobuf.Value> values = embeddingsStruct.getFieldsMap().get("values").getListValue().getValuesList();
                        return values.stream()
                                .map(com.google.protobuf.Value::getNumberValue)
                                .collect(Collectors.toList());
                    }
                }
            }
            
            logger.warn("No embeddings found in response for text length: {}", text.length());
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Error generating embeddings: {}", e.getMessage(), e);
            throw new IOException("Failed to generate embeddings", e);
        }
    }
    
    /**
     * Generate embeddings for query (optimized for retrieval)
     */
    public List<Double> generateQueryEmbeddings(String query) throws IOException {
        initializeClient();
        
        try {
            String endpoint = String.format("projects/%s/locations/%s/publishers/google/models/%s", 
                    projectId, location, embeddingModel);
            EndpointName endpointName = EndpointName.parse(endpoint);
            
            Struct.Builder instanceBuilder = Struct.newBuilder();
            instanceBuilder.putFields("content", com.google.protobuf.Value.newBuilder().setStringValue(query).build());
            instanceBuilder.putFields("task_type", com.google.protobuf.Value.newBuilder().setStringValue("RETRIEVAL_QUERY").build());
            
            com.google.protobuf.Value instance = com.google.protobuf.Value.newBuilder().setStructValue(instanceBuilder.build()).build();
            
            Struct.Builder parametersBuilder = Struct.newBuilder();
            com.google.protobuf.Value parameters = com.google.protobuf.Value.newBuilder().setStructValue(parametersBuilder.build()).build();
            
            PredictRequest request = PredictRequest.newBuilder()
                    .setEndpoint(endpointName.toString())
                    .addInstances(instance)
                    .setParameters(parameters)
                    .build();
            
            PredictResponse response = predictionServiceClient.predict(request);
            
            if (!response.getPredictionsList().isEmpty()) {
                com.google.protobuf.Value prediction = response.getPredictionsList().get(0);
                Struct predictionStruct = prediction.getStructValue();
                
                if (predictionStruct.containsFields("embeddings")) {
                    Struct embeddingsStruct = predictionStruct.getFieldsMap().get("embeddings").getStructValue();
                    if (embeddingsStruct.containsFields("values")) {
                        List<com.google.protobuf.Value> values = embeddingsStruct.getFieldsMap().get("values").getListValue().getValuesList();
                        return values.stream()
                                .map(com.google.protobuf.Value::getNumberValue)
                                .collect(Collectors.toList());
                    }
                }
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Error generating query embeddings: {}", e.getMessage(), e);
            // For demonstration purposes, return a mock embedding when Vertex AI fails
            logger.warn("Using fallback mock embedding for query due to Vertex AI error: {}", e.getMessage());
            return generateMockEmbedding(query);
        }
    }
    
    /**
     * Generate compliance answer using Gemini LLM
     */
    public String generateComplianceAnswer(String query, List<String> relevantChunks) throws IOException {
        initializeClient();
        
        try {
            String endpoint = String.format("projects/%s/locations/%s/publishers/google/models/%s", 
                    projectId, location, generationModel);
            EndpointName endpointName = EndpointName.parse(endpoint);
            
            // Build the prompt with retrieved context
            String prompt = buildCompliancePrompt(query, relevantChunks);
            
            // Create the instance for generation
            Struct.Builder instanceBuilder = Struct.newBuilder();
            
            // Add the prompt as a message
            Struct.Builder contentBuilder = Struct.newBuilder();
            contentBuilder.putFields("role", com.google.protobuf.Value.newBuilder().setStringValue("user").build());
            contentBuilder.putFields("parts", com.google.protobuf.Value.newBuilder()
                    .setListValue(com.google.protobuf.ListValue.newBuilder()
                            .addValues(com.google.protobuf.Value.newBuilder()
                                    .setStructValue(Struct.newBuilder()
                                            .putFields("text", com.google.protobuf.Value.newBuilder().setStringValue(prompt).build())
                                            .build())
                                    .build())
                            .build())
                    .build());
            
            instanceBuilder.putFields("contents", com.google.protobuf.Value.newBuilder()
                    .setListValue(com.google.protobuf.ListValue.newBuilder()
                            .addValues(com.google.protobuf.Value.newBuilder().setStructValue(contentBuilder.build()).build())
                            .build())
                    .build());
            
            com.google.protobuf.Value instance = com.google.protobuf.Value.newBuilder().setStructValue(instanceBuilder.build()).build();
            
            // Set generation parameters
            Struct.Builder parametersBuilder = Struct.newBuilder();
            parametersBuilder.putFields("temperature", com.google.protobuf.Value.newBuilder().setNumberValue(temperature).build());
            parametersBuilder.putFields("max_output_tokens", com.google.protobuf.Value.newBuilder().setNumberValue(maxTokens).build());
            parametersBuilder.putFields("top_p", com.google.protobuf.Value.newBuilder().setNumberValue(topP).build());
            parametersBuilder.putFields("top_k", com.google.protobuf.Value.newBuilder().setNumberValue(topK).build());
            
            com.google.protobuf.Value parameters = com.google.protobuf.Value.newBuilder().setStructValue(parametersBuilder.build()).build();
            
            // Make the prediction request
            PredictRequest request = PredictRequest.newBuilder()
                    .setEndpoint(endpointName.toString())
                    .addInstances(instance)
                    .setParameters(parameters)
                    .build();
            
            PredictResponse response = predictionServiceClient.predict(request);
            
            // Extract generated text from response
            if (!response.getPredictionsList().isEmpty()) {
                com.google.protobuf.Value prediction = response.getPredictionsList().get(0);
                Struct predictionStruct = prediction.getStructValue();
                
                if (predictionStruct.containsFields("candidates")) {
                    List<com.google.protobuf.Value> candidates = predictionStruct.getFieldsMap().get("candidates").getListValue().getValuesList();
                    if (!candidates.isEmpty()) {
                        Struct candidateStruct = candidates.get(0).getStructValue();
                        if (candidateStruct.containsFields("content")) {
                            Struct contentStruct = candidateStruct.getFieldsMap().get("content").getStructValue();
                            if (contentStruct.containsFields("parts")) {
                                List<com.google.protobuf.Value> parts = contentStruct.getFieldsMap().get("parts").getListValue().getValuesList();
                                if (!parts.isEmpty()) {
                                    Struct partStruct = parts.get(0).getStructValue();
                                    if (partStruct.containsFields("text")) {
                                        return partStruct.getFieldsMap().get("text").getStringValue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            logger.warn("No generated text found in response");
            return "Unable to generate compliance answer. Please try again.";
            
        } catch (Exception e) {
            logger.error("Error generating compliance answer: {}", e.getMessage(), e);
            // Provide fallback response when Vertex AI is not available
            return generateFallbackComplianceAnswer(query, relevantChunks, e);
        }
    }
    
    /**
     * Generate a general AI answer when no documents are available
     */
    public String generateGeneralAnswer(String query) throws IOException {
        initializeClient();
        
        try {
            logger.info("Generating general AI answer for query: {}", query);
            
            String endpoint = String.format("projects/%s/locations/%s/publishers/google/models/%s", 
                    projectId, location, generationModel);
            EndpointName endpointName = EndpointName.parse(endpoint);
            
            // Build general prompt for Gemini 2.5 Flash
            String prompt = buildGeneralPrompt(query);
            
            // Create the instance for generation (same structure as generateComplianceAnswer)
            Struct.Builder instanceBuilder = Struct.newBuilder();
            
            // Add the prompt as a message
            Struct.Builder contentBuilder = Struct.newBuilder();
            contentBuilder.putFields("role", com.google.protobuf.Value.newBuilder().setStringValue("user").build());
            contentBuilder.putFields("parts", com.google.protobuf.Value.newBuilder()
                    .setListValue(com.google.protobuf.ListValue.newBuilder()
                            .addValues(com.google.protobuf.Value.newBuilder()
                                    .setStructValue(Struct.newBuilder()
                                            .putFields("text", com.google.protobuf.Value.newBuilder().setStringValue(prompt).build())
                                            .build())
                                    .build())
                            .build())
                    .build());
            
            instanceBuilder.putFields("contents", com.google.protobuf.Value.newBuilder()
                    .setListValue(com.google.protobuf.ListValue.newBuilder()
                            .addValues(com.google.protobuf.Value.newBuilder().setStructValue(contentBuilder.build()).build())
                            .build())
                    .build());
            
            com.google.protobuf.Value instance = com.google.protobuf.Value.newBuilder().setStructValue(instanceBuilder.build()).build();
            
            // Set generation parameters
            Struct.Builder parametersBuilder = Struct.newBuilder();
            parametersBuilder.putFields("temperature", com.google.protobuf.Value.newBuilder().setNumberValue(temperature).build());
            parametersBuilder.putFields("max_output_tokens", com.google.protobuf.Value.newBuilder().setNumberValue(maxTokens).build());
            parametersBuilder.putFields("top_p", com.google.protobuf.Value.newBuilder().setNumberValue(topP).build());
            parametersBuilder.putFields("top_k", com.google.protobuf.Value.newBuilder().setNumberValue(topK).build());
            
            com.google.protobuf.Value parameters = com.google.protobuf.Value.newBuilder().setStructValue(parametersBuilder.build()).build();
            
            // Make the prediction request
            PredictRequest request = PredictRequest.newBuilder()
                    .setEndpoint(endpointName.toString())
                    .addInstances(instance)
                    .setParameters(parameters)
                    .build();
            
            PredictResponse response = predictionServiceClient.predict(request);
            
            if (!response.getPredictionsList().isEmpty()) {
                com.google.protobuf.Value prediction = response.getPredictionsList().get(0);
                Struct predictionStruct = prediction.getStructValue();
                
                if (predictionStruct.containsFields("candidates")) {
                    List<com.google.protobuf.Value> candidates = predictionStruct.getFieldsMap()
                            .get("candidates").getListValue().getValuesList();
                    
                    if (!candidates.isEmpty()) {
                        Struct candidate = candidates.get(0).getStructValue();
                        if (candidate.containsFields("content")) {
                            Struct content = candidate.getFieldsMap().get("content").getStructValue();
                            if (content.containsFields("parts")) {
                                List<com.google.protobuf.Value> parts = content.getFieldsMap()
                                        .get("parts").getListValue().getValuesList();
                                if (!parts.isEmpty()) {
                                    Struct part = parts.get(0).getStructValue();
                                    if (part.containsFields("text")) {
                                        String generatedText = part.getFieldsMap().get("text").getStringValue();
                                        logger.info("Generated general answer successfully");
                                        return generatedText;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            logger.warn("No generated text found in general response");
            return "I'm ready to help! However, I need a bit more context or a specific question to provide you with the most accurate assistance.";
            
        } catch (Exception e) {
            logger.error("Error generating general answer: {}", e.getMessage(), e);
            // Provide fallback response when Vertex AI is not available
            return generateFallbackGeneralAnswer(query, e);
        }
    }

    /**
     * Build a structured prompt for compliance queries with strict scoping for Gemini 2.5 Flash
     */
    private String buildCompliancePrompt(String query, List<String> relevantChunks) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("You are a specialized legal compliance assistant. Your ONLY task is to answer compliance questions using EXCLUSIVELY the provided policy document excerpts below.\n\n");
        
        promptBuilder.append("STRICT RULES:\n");
        promptBuilder.append("1. Use ONLY information found in the excerpts below\n");
        promptBuilder.append("2. If the excerpts don't contain sufficient information, clearly state: 'The provided excerpts do not contain enough information to answer this question'\n");
        promptBuilder.append("3. NEVER add external knowledge, assumptions, or general legal advice\n");
        promptBuilder.append("4. Quote specific text from excerpts when possible\n");
        promptBuilder.append("5. If no excerpts relate to the question, respond: 'No relevant information found in the provided documents'\n\n");
        
        promptBuilder.append("PROVIDED POLICY EXCERPTS:\n");
        promptBuilder.append("=".repeat(50)).append("\n");
        for (int i = 0; i < relevantChunks.size(); i++) {
            promptBuilder.append("[EXCERPT ").append(i + 1).append("]\n");
            promptBuilder.append(relevantChunks.get(i)).append("\n");
            promptBuilder.append("-".repeat(30)).append("\n");
        }
        promptBuilder.append("=".repeat(50)).append("\n\n");
        
        promptBuilder.append("USER QUESTION: ").append(query).append("\n\n");
        
        promptBuilder.append("RESPONSE FORMAT:\n");
        promptBuilder.append("- Start with a direct answer if found in excerpts\n");
        promptBuilder.append("- Quote relevant text: \"According to [Excerpt X]: [quoted text]\"\n");
        promptBuilder.append("- End with specific section/clause references if applicable\n");
        promptBuilder.append("- Use professional, compliance-focused language\n\n");
        
        promptBuilder.append("ANSWER (based ONLY on the excerpts above):\n");
        
        return promptBuilder.toString();
    }
    
    /**
     * Calculate cosine similarity between two embedding vectors
     */
    public double calculateCosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must be of same length");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Convert embedding list to comma-separated string for storage
     */
    public String embeddingToString(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
    
    /**
     * Convert comma-separated string back to embedding list
     */
    public List<Double> stringToEmbedding(String embeddingStr) {
        if (embeddingStr == null || embeddingStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(embeddingStr.split(","))
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }
    
    /**
     * Generate a fallback compliance answer when Vertex AI is not available
     */
    private String generateFallbackComplianceAnswer(String query, List<String> relevantChunks, Exception error) {
        return "Service unavailable. Please try again later.";
    }

    /**
     * Build a general prompt when no documents are available
     */
    private String buildGeneralPrompt(String query) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("You are a helpful and knowledgeable AI assistant powered by Gemini 2.5 Flash. ");
        promptBuilder.append("You can assist with a wide range of questions including general knowledge, explanations, advice, and problem-solving.\n\n");
        
        promptBuilder.append("GUIDELINES:\n");
        promptBuilder.append("- Provide accurate, helpful, and well-structured responses\n");
        promptBuilder.append("- If you're not certain about something, acknowledge the uncertainty\n");
        promptBuilder.append("- Use clear, professional language\n");
        promptBuilder.append("- Break down complex topics into understandable parts\n");
        promptBuilder.append("- Offer practical examples when relevant\n\n");
        
        promptBuilder.append("USER QUESTION: ").append(query).append("\n\n");
        
        promptBuilder.append("Please provide a comprehensive and helpful response:\n");
        
        return promptBuilder.toString();
    }

    /**
     * Generate a fallback general answer when Vertex AI is not available
     */
    private String generateFallbackGeneralAnswer(String query, Exception error) {
        return "Hello! Service is currently unavailable. Please try again later.";
    }

    /**
     * Generate a mock embedding for fallback purposes
     * This creates a simple pseudo-random embedding based on text content
     */
    private List<Double> generateMockEmbedding(String text) {
        // Create a 768-dimensional mock embedding (standard size for many models)
        List<Double> mockEmbedding = new ArrayList<>();
        
        // Use text hash as seed for reproducible but varied embeddings
        int seed = text.hashCode();
        Random random = new Random(seed);
        
        // Generate normalized random values
        for (int i = 0; i < 768; i++) {
            mockEmbedding.add(random.nextGaussian() * 0.1); // Small variance around 0
        }
        
        // Normalize the vector to unit length
        double norm = Math.sqrt(mockEmbedding.stream()
            .mapToDouble(x -> x * x)
            .sum());
        
        if (norm > 0) {
            for (int i = 0; i < mockEmbedding.size(); i++) {
                mockEmbedding.set(i, mockEmbedding.get(i) / norm);
            }
        }
        
        return mockEmbedding;
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        if (predictionServiceClient != null) {
            predictionServiceClient.close();
            logger.info("Vertex AI client shut down");
        }
    }
}