package com.example.demo.service;

import com.example.demo.dto.ComplianceQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory vector store for storing and retrieving document chunks with embeddings
 * In production, consider using a proper vector database like Pinecone, Weaviate, or Chroma
 */
@Service
public class VectorStoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);
    
    @Autowired
    private VertexAIService vertexAIService; // For embeddings only
    
    @Autowired
    private OpenAIService openAIService; // For chat generation
    
    @Autowired
    private DocumentProcessingService documentProcessingService;
    
    // In-memory storage for chunks and their embeddings
    // In production, this should be replaced with a persistent vector database
    private final Map<String, ChunkData> vectorStore = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> documentChunks = new ConcurrentHashMap<>();
    
    /**
     * Data structure to hold chunk information with embeddings
     */
    public static class ChunkData {
        private final String chunkId;
        private final Long documentId;
        private final String text;
        private final List<Double> embedding;
        private final Integer chunkIndex;
        private final List<String> clauseReferences;
        
        public ChunkData(String chunkId, Long documentId, String text, List<Double> embedding, 
                        Integer chunkIndex, List<String> clauseReferences) {
            this.chunkId = chunkId;
            this.documentId = documentId;
            this.text = text;
            this.embedding = embedding;
            this.chunkIndex = chunkIndex;
            this.clauseReferences = clauseReferences;
        }
        
        // Getters
        public String getChunkId() { return chunkId; }
        public Long getDocumentId() { return documentId; }
        public String getText() { return text; }
        public List<Double> getEmbedding() { return embedding; }
        public Integer getChunkIndex() { return chunkIndex; }
        public List<String> getClauseReferences() { return clauseReferences; }
    }
    
    /**
     * Store document chunks with their embeddings
     */
    public List<String> storeDocumentChunks(Long documentId, String documentName, List<String> chunks) throws IOException {
        logger.info("Storing {} chunks for document: {}", chunks.size(), documentName);
        
        Set<String> chunkIds = new HashSet<>();
        List<String> failedChunks = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkId = generateChunkId(documentId, i);
            
            try {
                // Generate embeddings for the chunk
                List<Double> embedding = null;
                try {
                    embedding = vertexAIService.generateEmbeddings(chunk);
                } catch (Exception embeddingError) {
                    logger.warn("Vertex AI embedding generation failed for chunk {} of document {}: {}. Using fallback mock embedding.", 
                               i, documentId, embeddingError.getMessage());
                    // Fallback: Create a mock embedding for development/demo purposes
                    embedding = generateMockEmbedding(chunk);
                }
                
                if (embedding == null || embedding.isEmpty()) {
                    logger.warn("Empty or null embedding generated for chunk {} of document {}", i, documentId);
                    failedChunks.add(chunk);
                    continue;
                }
                
                // Extract clause references from chunk text
                List<String> clauseReferences = documentProcessingService.extractClauseReferences(chunk);
                
                // Create and store chunk data
                ChunkData chunkData = new ChunkData(chunkId, documentId, chunk, embedding, i, clauseReferences);
                vectorStore.put(chunkId, chunkData);
                chunkIds.add(chunkId);
                
                logger.debug("Stored chunk {}/{} for document {} with {} clause references", 
                           i + 1, chunks.size(), documentId, clauseReferences.size());
                
            } catch (Exception e) {
                logger.error("Failed to process chunk {} of document {}: {}", i, documentId, e.getMessage());
                failedChunks.add(chunk);
            }
        }
        
        // Store document-to-chunks mapping
        documentChunks.put(documentId, chunkIds);
        
        logger.info("Successfully stored {}/{} chunks for document {}", 
                   chunkIds.size(), chunks.size(), documentId);
        
        return failedChunks;
    }
    
    /**
     * Retrieve most similar chunks for a given query
     */
    public List<ChunkData> retrieveSimilarChunks(String query, int maxResults) throws IOException {
        logger.debug("Retrieving similar chunks for query: {}", query.substring(0, Math.min(query.length(), 50)) + "...");
        
        if (vectorStore.isEmpty()) {
            logger.warn("Vector store is empty. No chunks to search.");
            return new ArrayList<>();
        }
        
        // Generate query embeddings
        List<Double> queryEmbedding = vertexAIService.generateQueryEmbeddings(query);
        
        if (queryEmbedding.isEmpty()) {
            logger.error("Failed to generate query embeddings");
            return new ArrayList<>();
        }
        
        // Calculate similarity scores for all chunks
        List<ChunkSimilarity> similarities = new ArrayList<>();
        
        for (ChunkData chunk : vectorStore.values()) {
            try {
                double similarity = vertexAIService.calculateCosineSimilarity(queryEmbedding, chunk.getEmbedding());
                similarities.add(new ChunkSimilarity(chunk, similarity));
            } catch (Exception e) {
                logger.warn("Failed to calculate similarity for chunk {}: {}", chunk.getChunkId(), e.getMessage());
            }
        }
        
        // Sort by similarity score (descending) and return top results
        List<ChunkData> topChunks = similarities.stream()
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(maxResults)
                .map(cs -> cs.chunkData)
                .collect(Collectors.toList());
        
        logger.debug("Retrieved {} similar chunks with similarities: {}", 
                   topChunks.size(),
                   similarities.stream()
                           .limit(maxResults)
                           .map(cs -> String.format("%.3f", cs.similarity))
                           .collect(Collectors.joining(", ")));
        
        return topChunks;
    }
    
    /**
     * Search for compliance information and generate answer
     */
    public ComplianceQueryResponse searchCompliance(String query, int maxResults) {
        try {
            logger.info("Processing compliance query: {}", query);
            
            // Retrieve similar chunks
            List<ChunkData> similarChunks = retrieveSimilarChunks(query, maxResults);
            
            if (similarChunks.isEmpty()) {
                logger.info("No relevant policy documents found.");
                // Use OpenAI for general chat when no documents are available
                String generalAnswer = openAIService.generateGeneralAnswer(query);
                return new ComplianceQueryResponse(generalAnswer, new ArrayList<>());
            }
            
            // Extract text from similar chunks
            List<String> chunkTexts = similarChunks.stream()
                    .map(ChunkData::getText)
                    .collect(Collectors.toList());
            
            // Generate compliance answer using OpenAI
            String answer = openAIService.generateComplianceAnswer(query, chunkTexts);
            
            // Extract clause references from all similar chunks
            Set<String> allClauseReferences = new HashSet<>();
            for (ChunkData chunk : similarChunks) {
                allClauseReferences.addAll(chunk.getClauseReferences());
            }
            
            // If no clause references found in chunks, try to extract from the generated answer
            if (allClauseReferences.isEmpty()) {
                List<String> answerClauses = documentProcessingService.extractClauseReferences(answer);
                allClauseReferences.addAll(answerClauses);
            }
            
            List<String> referencedClauses = new ArrayList<>(allClauseReferences);
            
            logger.info("Generated compliance answer with {} referenced clauses", referencedClauses.size());
            
            return new ComplianceQueryResponse(answer, referencedClauses);
            
        } catch (IOException e) {
            logger.error("Error processing compliance query: {}", e.getMessage(), e);
            return ComplianceQueryResponse.error("Failed to process compliance query: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing compliance query: {}", e.getMessage(), e);
            return ComplianceQueryResponse.error("An unexpected error occurred while processing your query.");
        }
    }
    
    /**
     * Remove all chunks for a specific document
     */
    public void removeDocumentChunks(Long documentId) {
        Set<String> chunkIds = documentChunks.get(documentId);
        if (chunkIds != null) {
            chunkIds.forEach(vectorStore::remove);
            documentChunks.remove(documentId);
            logger.info("Removed {} chunks for document {}", chunkIds.size(), documentId);
        }
    }
    
    /**
     * Get statistics about the vector store
     */
    public Map<String, Object> getVectorStoreStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChunks", vectorStore.size());
        stats.put("totalDocuments", documentChunks.size());
        
        // Calculate average chunks per document
        if (!documentChunks.isEmpty()) {
            double avgChunksPerDoc = documentChunks.values().stream()
                    .mapToInt(Set::size)
                    .average()
                    .orElse(0.0);
            stats.put("averageChunksPerDocument", avgChunksPerDoc);
        }
        
        return stats;
    }
    
    /**
     * Clear all stored vectors (useful for testing)
     */
    public void clearAll() {
        vectorStore.clear();
        documentChunks.clear();
        logger.info("Cleared all vectors from store");
    }
    
    /**
     * Generate unique chunk ID
     */
    private String generateChunkId(Long documentId, int chunkIndex) {
        return String.format("doc_%d_chunk_%d", documentId, chunkIndex);
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
     * Helper class to hold chunk data with similarity score
     */
    private static class ChunkSimilarity {
        private final ChunkData chunkData;
        private final double similarity;
        
        public ChunkSimilarity(ChunkData chunkData, double similarity) {
            this.chunkData = chunkData;
            this.similarity = similarity;
        }
    }
}