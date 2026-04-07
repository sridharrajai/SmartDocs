# SmartDocs RAG API

A production-style REST API that accepts PDF documents, chunks and embeds them into a vector database, and answers questions grounded strictly in the uploaded content — eliminating hallucination by design.

Built with Java 21 · Spring Boot 3 · LangChain4j · Spring AI · OpenAI · Qdrant · PostgreSQL · Redis

> **Educational project.** Each layer is intentionally explicit so the full request lifecycle — from PDF upload to council-refined answer — is easy to follow and learn from.

---

## Architecture

![Architecture Diagram](<Project%20Architecture.png>)

The request flows through seven distinct layers

## Two core flows

**Ingestion**
PDF → `PagePdfDocumentReader` → `SlidingWindowSplitter` (512 tokens, 50 overlap) → embed via `text-embedding-3-small` → store vectors in Qdrant → save metadata to PostgreSQL

**Agent chat**
Question → `AgentService` (loads token-trimmed history) → `DocumentAssistant` triggers `KnowledgeBaseTools` → similarity search in Qdrant (top 3, threshold 0.5) → context stored in `AgentContextHolder` → `CouncilOrchestrator` runs Critic → Refiner if needed → refined answer persisted to PostgreSQL

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Agent Framework | LangChain4j 1.12 |
| AI / Embeddings | Spring AI 1.1 |
| LLM | OpenAI gpt-4o-mini |
| Embedding Model | text-embedding-3-small |
| Vector Store | Qdrant |
| Relational DB | PostgreSQL 16 |
| Session Cache | Redis |
| Tokeniser | jtokkit (CL100K_BASE) |
| Containerisation | Docker / docker-compose |

---

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/agent/chat` | Multi-turn agent chat with RAG + council refinement |
| POST | `/api/v1/ingest` | Upload a PDF — chunks, embeds, stores |
| POST | `/api/v1/ask` | Direct RAG Q&A (no agent, no council) |
| POST | `/api/v1/chat` | Plain LLM chat (no RAG) |
| GET | `/api/v1/documents` | List all ingested documents |
| GET | `/api/v1/chat/history` | Retrieve chat history for a session |
| GET | `/` | Health check |

---

## Run Locally

### Prerequisites
- Java 21
- Docker
- OpenAI API key

### 1. Start infrastructure

```bash
docker-compose up -d
```

Starts PostgreSQL on `5432` and Qdrant on `6333` / `6334`. Add Redis (`redis:7`) to `docker-compose.yml` if not already present.

### 2. Set your API key

```bash
# Windows
set OPENAI_API_KEY=your_key_here

# Mac / Linux
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

```json
{
  "filename": "document.pdf",
  "chunksStored": 24,
  "status": "COMPLETED"
}
```

### Agent chat (multi-turn, with council)

```bash
curl -X POST http://localhost:8081/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: my-session" \
  -H "X-User-Id: user-1" \
  -d '{"query": "What are the key findings in this document?"}'
```

```json
{
  "answer": "According to the document, the key findings are...",
  "councilUsed": true
}
```

### Direct RAG ask

```bash
curl -X POST http://localhost:8081/api/v1/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the main topics covered?"}'
```

---

## Error Handling

All errors return RFC-7807 Problem Details:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Prompt injection detected."
}
```

Prompt injection triggers a 400 immediately at the filter stage, before any LLM call is made.

---

## Project Structure

```
src/main/java/com/sridhar/ragapi/
├── agent/
│   ├── DocumentAssistant.java       # LangChain4j @AiService interface
│   ├── KnowledgeBaseTool.java       # @Tool — Qdrant similarity search
│   └── AgentContextHolder.java      # ThreadLocal RAG context carrier
├── config/
│   ├── LangChain4jConfig.java       # ChatModel + ChatMemoryProvider beans
│   └── CorsConfig.java
├── controller/
│   ├── AgentController.java         # POST /api/v1/agent/chat
│   ├── ChatController.java          # POST /api/v1/chat + /ask
│   ├── IngestController.java        # POST /api/v1/ingest
│   ├── HistoryController.java       # GET  /api/v1/chat/history
│   └── HealthController.java
├── council/
│   └── CouncilOrchestrator.java     # Critic → Refiner pipeline
├── entity/
│   ├── ChatMessage.java
│   ├── IngestedDocument.java
│   └── MessageRole.java
├── exception/
│   ├── GlobalExceptionHandler.java  # RFC-7807 error responses
│   ├── ErrorResponse.java
│   └── PromptInjectionException.java
├── repository/
│   ├── ChatMessageRepository.java
│   └── IngestedDocumentRepository.java
├── service/
│   ├── AgentService.java            # Orchestrates agent + council
│   ├── ChatService.java             # Direct LLM / RAG calls
│   ├── IngestService.java           # PDF → chunks → Qdrant
│   ├── ChatHistoryService.java      # PostgreSQL persistence
│   ├── SessionCacheService.java     # Redis session cache
│   ├── SessionManager.java
│   └── TokenAwareMemoryService.java # Token-budget history trimming
└── util/
    ├── AskRequest.java
    ├── AgentRequest.java
    └── SlidingWindowSplitter.java   # jtokkit-accurate chunker
```

---

## Environment Variables

| Variable | Description | Required |
|---|---|---|
| `OPENAI_API_KEY` | OpenAI API key | Yes |

---

## Infrastructure (docker-compose)

```yaml
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
  redis:
    image: redis:7
    ports:
      - "6379:6379"
```

Run with: `docker-compose up -d`
