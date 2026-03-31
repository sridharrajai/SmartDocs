# SmartDocs RAG API

A production-style REST API that accepts PDF documents, chunks and embeds them into a vector database, and answers questions grounded strictly in the uploaded content — eliminating hallucination by design.

Built with Java 21 · Spring Boot 3 · Spring AI · OpenAI · Qdrant · PostgreSQL



---

## Architecture


![Architecture Diagram](<Project%20Architecture.png>)
Two flows:

**Ingestion** — PDF → chunk (512 tokens, 50 overlap) → embed via OpenAI text-embedding-3-small → store vectors in Qdrant → save metadata to PostgreSQL

**Retrieval** — question → embed → cosine similarity search in Qdrant (top 3, threshold 0.6) → inject chunks into prompt template → LLM answers only from retrieved context

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| AI Framework | Spring AI 1.1.x |
| LLM | OpenAI gpt-4o-mini |
| Embedding Model | text-embedding-3-small |
| Vector Store | Qdrant |
| Relational DB | PostgreSQL 16 |
| Containerisation | Docker |

---

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/ingest` | Upload a PDF — chunks, embeds, stores |
| POST | `/api/v1/ask` | Ask a question — grounded answer only |
| GET | `/api/v1/documents` | List all ingested documents |

---

## Run Locally

### Prerequisites
- Java 21
- Docker
- OpenAI API key (or Google Gemini API key — free)

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL on port 5432 and Qdrant on port 6333.

### 2. Set your API key

```bash
# Windows
set OPENAI_API_KEY=your_key_here

# Mac/Linux
export OPENAI_API_KEY=your_key_here
```

### 3. Run the app

```bash
./mvnw spring-boot:run
```

App starts on `http://localhost:8081`

---

## Example Requests

### Ingest a PDF

```bash
curl -X POST http://localhost:8081/api/v1/ingest \
  -F "file=@/path/to/document.pdf"
```

Response:
```json
{
  "filename": "document.pdf",
  "chunksStored": 24,
  "status": "COMPLETED"
}
```

### Ask a question

```bash
curl -X POST http://localhost:8081/api/v1/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the main topics covered in this document?"}'
```

Response:
```json
"Based on the document, the main topics covered are..."
```

If the answer is not in the document:
```json
"I don't have that information."
```

### List ingested documents

```bash
curl http://localhost:8081/api/v1/documents
```

---

## Key Design Decisions

**Why Qdrant?** Self-hosted, fast, no per-query cost at scale. Runs in Docker with zero config.

**Why 512-token chunks with 50-token overlap?** Overlap prevents sentences split across chunk boundaries from losing context. 512 tokens balances retrieval precision against context richness.

**Why similarity threshold 0.5?** Filters low-relevance chunks before they reach the prompt. Prevents noise from degrading answer quality.

**Why answer-only-from-context prompt?** The model is explicitly instructed not to use its training data. If the context is empty or irrelevant, it says so. Hallucination is structurally prevented, not just hoped against.

---

## Error Handling

All errors return RFC-7807 Problem Details format:

```json
{
  "type": "about:blank",
  "title": "Internal Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please try again."
}
```

---

## Project Structure

```
src/main/java/com/sridhar/ragapi/
├── controller/
│   └── ChatController.java
│   
├── service/
│   ├── IngestService.java
│   └── ChatService.java
├── entity/
│   └── IngestedDocument.java
├── repository/
│   └── IngestedDocumentRepository.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── PromptInjectionException.java
└── config/
    └── CorsConfig.java
```

---

## Environment Variables

| Variable | Description | Required |
|---|---|---|
| `OPENAI_API_KEY` | OpenAI API key | Yes (or Gemini) |
| `GEMINI_API_KEY` | Google Gemini API key | Yes (or OpenAI) |

---

## docker-compose.yml

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: smartdocs
    ports:
      - "5432:5432"
  qdrant:
    image: qdrant/qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
```

Run with: `docker-compose up -d`
