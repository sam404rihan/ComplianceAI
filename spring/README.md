# Legal/Policy Compliance Checker Backend

A Spring Boot application that provides RAG (Retrieval-Augmented Generation) based compliance checking using Google Vertex AI Gemini LLM.

## Features

- **Document Upload**: Upload PDF/DOCX policy documents
- **Text Processing**: Extract and chunk text from documents (~500 tokens per chunk)
- **Vector Embeddings**: Generate embeddings using Vertex AI Text Embeddings API
- **Semantic Search**: Find relevant policy clauses using vector similarity
- **AI-Powered Answers**: Generate compliance answers using Gemini LLM
- **RESTful API**: Easy integration with frontend applications

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Client App    │────│  Spring Boot     │────│   Vertex AI     │
│   (Frontend)    │    │   Backend        │    │    (Gemini)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                       ┌──────────────────┐
                       │  Vector Store    │
                       │  (In-Memory)     │
                       └──────────────────┘
```

## Prerequisites

1. **Java 17** or higher
2. **Maven 3.6+**
3. **Google Cloud Project** with Vertex AI API enabled
4. **Google Cloud Authentication** (Service Account Key or Application Default Credentials)

## Setup Instructions

### 1. Google Cloud Setup

1. Create a Google Cloud Project
2. Enable Vertex AI API
3. Create a Service Account with Vertex AI permissions
4. Download the service account JSON key

### 2. Application Configuration

1. Clone or download the project
2. Update `src/main/resources/application.properties`:

```properties
# Replace with your actual values
google.cloud.project-id=your-google-cloud-project-id
google.cloud.location=us-central1
# google.cloud.credentials.location=/path/to/service-account-key.json
```

3. Set up authentication (choose one):

**Option A: Environment Variable**
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
```

**Option B: Application Properties**
```properties
google.cloud.credentials.location=/path/to/service-account-key.json
```

### 3. Build and Run

```bash
# Install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Upload Document
```http
POST /api/compliance/upload
Content-Type: multipart/form-data

file: [PDF or DOCX file]
```

**Response:**
```json
{
  "message": "Document uploaded and processed successfully. 25 chunks created.",
  "documentId": 1703123456789,
  "documentName": "company-policy.pdf",
  "chunksCreated": 25,
  "status": "success"
}
```

### Query Compliance
```http
POST /api/compliance/query
Content-Type: application/json

{
  "query": "What is the policy on remote work?",
  "maxResults": 5
}
```

**Response:**
```json
{
  "answer": "According to Clause 5.2, employees may work remotely up to 2 days per week with manager approval. All remote work must comply with data security requirements outlined in Section 3.1.",
  "referenced_clauses": ["Clause 5.2", "Section 3.1"],
  "status": "success"
}
```

### Health Check
```http
GET /api/compliance/health
```

### Statistics
```http
GET /api/compliance/stats
```

## Configuration Options

### Document Processing
```properties
# Chunk size in tokens (default: 500)
document.chunk.size=500

# Overlap between chunks in tokens (default: 50)
document.chunk.overlap=50

# Maximum file size (default: 10MB)
document.max-file-size=10MB
```

### Vertex AI Models
```properties
# Embedding model (default: text-embedding-004)
vertex.ai.model.embedding=text-embedding-004

# Generation model (default: gemini-1.5-flash)
vertex.ai.model.generation=gemini-1.5-flash

# Maximum output tokens (default: 8192)
vertex.ai.max-tokens=8192

# Temperature for generation (default: 0.2)
vertex.ai.temperature=0.2
```

## Usage Examples

### 1. Upload a Policy Document

```bash
curl -X POST "http://localhost:8080/api/compliance/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/employee-handbook.pdf"
```

### 2. Query Compliance Information

```bash
curl -X POST "http://localhost:8080/api/compliance/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the requirements for taking vacation leave?",
    "maxResults": 5
  }'
```

### 3. Check Application Health

```bash
curl -X GET "http://localhost:8080/api/compliance/health"
```

## Supported File Types

- **PDF**: Uses Apache PDFBox for text extraction
- **DOCX**: Uses Apache POI for text extraction

## Error Handling

The application includes comprehensive error handling for:

- Invalid file types
- File size exceeds limits
- Vertex AI API errors
- Authentication failures
- Network connectivity issues
- Malformed requests

## Security Considerations

- File uploads are validated for type and size
- Input validation on all API endpoints
- Secure handling of Google Cloud credentials
- CORS configuration for frontend integration

## Logging

The application uses SLF4J with Logback for structured logging:

- `INFO` level for general application flow
- `DEBUG` level for detailed processing information
- `ERROR` level for exceptions and failures

## Production Deployment

For production deployment, consider:

1. **Replace In-Memory Vector Store** with a persistent solution:
   - PostgreSQL with pgvector extension
   - Pinecone, Weaviate, or Chroma vector databases

2. **Use Database for Document Storage**:
   - Add JPA entities for persistent document storage
   - Configure PostgreSQL or MySQL database

3. **Add Authentication/Authorization**:
   - Spring Security integration
   - JWT token validation
   - Role-based access control

4. **Configure External Configuration**:
   - Use environment variables
   - External configuration service (Spring Cloud Config)

5. **Add Monitoring**:
   - Spring Boot Actuator
   - Prometheus/Grafana metrics
   - Health checks and alerts

6. **Optimize Performance**:
   - Connection pooling for Vertex AI
   - Caching for frequently accessed embeddings
   - Async processing for large documents

## Troubleshooting

### Common Issues

1. **Authentication Error**:
   ```
   Error: UNAUTHENTICATED: Request had invalid authentication credentials
   ```
   - Verify GOOGLE_APPLICATION_CREDENTIALS is set correctly
   - Check service account has Vertex AI permissions

2. **Project Not Found**:
   ```
   Error: Project 'your-project-id' not found
   ```
   - Verify google.cloud.project-id in application.properties
   - Ensure project exists and Vertex AI API is enabled

3. **File Upload Fails**:
   ```
   Error: File size exceeds maximum allowed size
   ```
   - Check file is under 10MB limit
   - Verify file type is PDF or DOCX

### Debug Mode

Run with debug logging:
```bash
mvn spring-boot:run -Dspring.profiles.active=debug
```

Or set in application.properties:
```properties
logging.level.com.example.demo=DEBUG
```

## License

This project is licensed under the MIT License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review application logs
3. Verify Google Cloud configuration
4. Test with simple examples first