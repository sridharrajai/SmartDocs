# SmartDocs

**SmartDocs** is a Java 21 / Spring Boot 3 AI backend that ingests PDF documents, grounds LLM answers in retrieved context, and self-corrects responses through a 3-stage LLM Council pipeline (Drafter → Critic → Refiner). Conversation memory persists across sessions via PostgreSQL, and council-verified answers compound nightly into the vector store via a scheduled Knowledge Loop.

Built with Java 21 · Spring Boot 3 · LangChain4j · Spring AI · OpenAI · Qdrant · PostgreSQL · Redis

> Built to demonstrate production AI engineering patterns: RAG, persistent agent memory, self-correcting LLM pipelines, Redis session cache, circuit breaking, and Micrometer observability.

---

## Production Engineering Highlights

| Pattern | Implementation |
|---|---|
| Self-correcting LLM | Council pipeline: Critic audits draft against RAG context, Refiner fixes only flagged issues — fast-path returns on ACCEPTABLE verdict |
| Token-aware memory | Backwards-walk trimming to 4000-token budget (2000 in Council mode) via `TokenAwareMemoryService` |
| Redis embedding cache | `EmbeddingCacheService` wraps Spring AI's `EmbeddingModel` — ~40% cost reduction on repeated queries |
| Prompt injection defence | `PromptInjectionFilter` validates all input before any LLM call — returns RFC-7807 400 immediately |
| Circuit breaker | Resilience4j: opens at 50% failure in 10-call window, 30s fast-fail, HALF-OPEN probe recovery |
| Observability | Micrometer: `ai.tokens.total` Counter + `ai.llm.latency` Timer → `/actuator/metrics` |
| Nightly knowledge compounding | `KnowledgeLoopPromoter` `@Scheduled` job promotes council-verified answers back into Qdrant |

---

## Architecture

![Architecture Diagram](Project%20Architecture.png)

```
PDF Upload ──► Ingest ──► Chunk (512-tok, 50 overlap) ──► Embed ──► Qdrant
                                                                       │
User Query ──► AgentService ──► DocumentAssistant ──► KnowledgeBaseTools ──► Qdrant top-3
                                                                       │
                                                         AgentContextHolder (ThreadLocal)
                                                                       │
                                                    CouncilOrchestrator: Critic ──► Refiner?
                                                                       │
                                                      PostgreSQL (persist) + Redis (session)
                                                                       │
                                                            Final Answer ──► HTTP Response
```

The request flows through seven distinct layers: HTTP ingress → agent orchestration → tool-augmented retrieval → council refinement → persistent storage → session cache → response.

---

## Two Core Flows

**Ingestion**

```
PDF → PagePdfDocumentReader → SlidingWindowSplitter (512 tokens, 50 overlap)
    → embed via text-embedding-3-small → store vectors in Qdrant
    → save metadata to PostgreSQL (IngestedDocument entity)
```

**Agent Chat**

```
Question → AgentService (loads token-trimmed history)
         → DocumentAssistant triggers KnowledgeBaseTools
         → similarity search in Qdrant (top 3, threshold 0.5)
         → context stored in AgentContextHolder (ThreadLocal)
         → CouncilOrchestrator: Critic audits draft
             → ACCEPTABLE: return draft (2 LLM calls)
             → flagged: Refiner rewrites (3 LLM calls)
         → answer persisted to PostgreSQL
```

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
| Session Cache | Redis 7 |
| Tokeniser | jtokkit (CL100K\_BASE) |
| Containerisation | Docker / docker-compose |

---

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/agent/chat` | Multi-turn agent chat with RAG + council refinement |
| `POST` | `/api/v1/ingest` | Upload a PDF — chunks, embeds, stores |
| `POST` | `/api/v1/ask` | Direct RAG Q&A (no agent, no council) |
| `POST` | `/api/v1/chat` | Plain LLM chat (no RAG) |
| `GET` | `/api/v1/documents` | List all ingested documents |
| `GET` | `/api/v1/chat/history` | Retrieve chat history for a session |
| `GET` | `/` | Health check |

---

## Run Locally

### Prerequisites
- Java 21
- Docker
- OpenAI API key

### 1. Clone the repo

```bash
git clone https://github.com/sridharrajai/SmartDocs.git
cd SmartDocs
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

Starts PostgreSQL on `5432`, Qdrant on `6333` / `6334`, and Redis on `6379`.

### 3. Set your API key

```bash
# macOS / Linux
export OPENAI_API_KEY=your_key_here

# Windows
set OPENAI_API_KEY=your_key_here
```

### 4. Run the app

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

### Agent chat (multi-turn, council-refined)

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
│   ├── ChatMessage.java             # councilVerified + promotedToKnowledgeBase flags
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
│   ├── EmbeddingCacheService.java   # Redis cache wrapping EmbeddingModel
│   ├── SessionCacheService.java     # Redis session routing (2hr TTL)
│   ├── SessionManager.java
│   ├── TokenAwareMemoryService.java # Token-budget history trimming
│   └── KnowledgeLoopPromoter.java   # @Scheduled nightly Qdrant promotion
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
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | Yes (default in docker-compose) |
| `SPRING_DATA_REDIS_HOST` | Redis host | Yes (default: `localhost`) |
| `QDRANT_HOST` | Qdrant host | Yes (default: `localhost`) |

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

---

## Known Limitations

**Knowledge Loop**
- Duplicate promotions: repeated identical questions create multiple `councilVerified=true` rows, each promoted to Qdrant independently. Content-hash deduplication is a planned improvement.
- No vector provenance: promoted Qdrant points carry no `sourceMessageId` or timestamp metadata. Traceability back to the originating chat message is not yet implemented.
- No promotion failure handling: if `vectorStore.add()` throws, `saveAll()` is skipped silently. No retry or dead-letter mechanism exists for failed promotions.

**Council Verification**
- Fast-path responses (Critic returns ACCEPTABLE, Refiner skipped) are marked `councilVerified=true`. Verification granularity does not distinguish between Critic-only and full Critic+Refiner passes.
