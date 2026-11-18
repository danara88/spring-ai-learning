# MedAssistant

An AI-powered medical assistant API built with **Spring Boot 4** and **Spring AI 2**. It brings together the main patterns used in LLM applications: RAG, conversational memory, structured output, tool calling, agentic workflows, and MCP (Model Context Protocol). All of it sits behind a JWT-secured REST API with role-based access through Keycloak.

> ⚠️ **Disclaimer:** This is an educational project. It is not a medical device, and its output must never replace advice from a qualified healthcare professional.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Configuration & Profiles](#configuration--profiles)
- [Authentication](#authentication)
- [API Reference](#api-reference)
- [MCP Server](#mcp-server)
- [Project Structure](#project-structure)
- [Docker / Production Build](#docker--production-build)

---

## Features

- **Multi-model support:** switch between **OpenAI (gpt-4o)** and **Ollama (llama3.2)** on each request with the `model` field.
- **RAG (Retrieval-Augmented Generation):** medical documents (PDF, DOCX, etc.) are ingested with Apache Tika, split into chunks, embedded, and stored in a vector store. `QuestionAnswerAdvisor` then adds the relevant chunks to each prompt.
- **Conversational memory:** chat history is stored in JDBC (PostgreSQL), one conversation per authenticated user.
- **Streaming responses:** Server-Sent Events (`text/event-stream`) for chat and orchestration endpoints.
- **Structured output:** the LLM returns typed Java records (condition summaries, symptom analysis with severity/urgency, query classification).
- **Prompt engineering:** externalized StringTemplate prompts (`.st`), including chain-of-thought diagnosis and role-aware system prompts.
- **Tool calling:** the LLM can query patients, doctors, appointments, and drug information. Drug data comes live from the **openFDA API**.
- **Agentic workflows:**
  - **Prompt chaining:** appointment booking (extract → search → confirm/cancel).
  - **Routing:** classifies the query (emergency / symptom / prescription / general) and sends it to a specialized prompt.
  - **Orchestrator-workers:** a coordinator delegates to clinical, bibliography, and history workers, then combines their answers.
  - **Open agent:** an autonomous agent that decides which tools to use.
- **MCP:** the app is an **MCP server** (exposes resources and prompts at `/mcp`) and also an **MCP client** (filesystem, GitHub, and other stdio servers).
- **Custom advisor:** `MedicalAuditAdvisor` logs every LLM interaction for auditing.
- **Security:** OAuth2 resource server that validates Keycloak JWTs, with `PATIENT` and `ADMIN` roles.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1, Spring AI 2.0 |
| LLM providers | OpenAI (`gpt-4o`, `text-embedding-3-large`), Ollama (`llama3.2:1b`, `nomic-embed-text`) |
| Vector store | ChromaDB (local), PgVector (AWS profile) |
| Database | PostgreSQL + Spring Data JPA |
| Security | Spring Security OAuth2 Resource Server + Keycloak 26 |
| Document parsing | Apache Tika |
| Build | Maven (wrapper included) |
| Containers | Docker, Docker Compose |

## Architecture

```
                ┌──────────────┐
  Client ──JWT──►  Spring Security (Keycloak JWT → ROLE_PATIENT / ROLE_ADMIN)
                └──────┬───────┘
                       ▼
   Controllers  /api/v1/{chat, analysis, agent, documents, mcp}
                       ▼
   Services / Agents ──► ClientResolver(model) ──► ChatClient (openai | ollama)
                                                    │  Advisors:
                                                    │   • QuestionAnswerAdvisor (RAG)
                                                    │   • MessageChatMemoryAdvisor (JDBC, per chat call)
                                                    │   • MedicalAuditAdvisor (logging)
                                                    ▼
                                    Tools (@Tool) ──► JPA repos / openFDA API
                                    VectorStore   ──► Chroma | PgVector
```

The `model` field on each request (`"openai"` or `"ollama"`) chooses the `ChatClient` and the matching vector store. Each embedding model has its own collection, because vectors from different models can't be compared.

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose
- An OpenAI API key
- *(Optional)* [Ollama](https://ollama.com/) to run models locally
- *(Optional)* Node.js with the MCP servers `@modelcontextprotocol/server-filesystem` and `@modelcontextprotocol/server-github` installed globally, for the MCP client features

### 1. Start the infrastructure

```bash
docker compose up -d
```

| Service | Port | Purpose |
|---|---|---|
| PostgreSQL (app) | `5432` | Application data + chat memory |
| PostgreSQL (Keycloak) | `5433` | Keycloak storage |
| Keycloak | `8090` | Identity provider (admin / admin) |
| ChromaDB | `8000` | Vector store |

### 2. Configure Keycloak

1. Open http://localhost:8090 and sign in with `admin` / `admin`.
2. Create a realm named **`medassistant`**.
3. Create the realm roles **`PATIENT`** and **`ADMIN`**.
4. Create a client (for example `medassistant-client`) with *Direct access grants* turned on.
5. Create users and assign them roles.
6. Add a **`userId`** attribute to each user, and a client mapper (*User Attribute*, type `long`) that puts it in the token as the `userId` claim. Chat memory and appointment booking use this claim.

### 3. (Optional) Pull Ollama models

```bash
ollama pull llama3.2:1b
ollama pull nomic-embed-text
```

### 4. Set environment variables

```bash
export OPEN_AI_API_KEY=sk-...
export GITHUB_TOKEN=ghp_...   # only needed for the GitHub MCP client
```

### 5. Run the application

```bash
./mvnw spring-boot:run
```

The API starts at http://localhost:8080. On startup, `DataLoader` loads sample doctors (Cardiology, Dermatology, Pediatrics, Traumatology, Endocrinology), appointments, and patients.

### Running tests

```bash
./mvnw test                          # all tests (requires the infrastructure running)
./mvnw test -Dtest=ClassName#method  # a single test
```

## Configuration & Profiles

| Profile | LLM | Vector store | Notes |
|---|---|---|---|
| *default* | OpenAI + Ollama | ChromaDB | Local development. Schema is recreated on every start (`create-drop`). |
| `aws` | OpenAI only | PgVector | Production-like. Reads DB settings from env vars, MCP client disabled, schema kept (`update`). |

Run the `aws` profile locally against a pgvector container:

```bash
docker compose -f docker-compose.aws-test.yml up -d   # pgvector on port 5434
RDS_PORT=5434 ./mvnw spring-boot:run -Dspring-boot.run.profiles=aws
```

Environment variables for the `aws` profile: `RDS_HOST`, `RDS_PORT`, `RDS_DB`, `RDS_USERNAME`, `RDS_PASSWORD`, `KEYCLOAK_ISSUER_URI`, `OPEN_AI_API_KEY`.

> **Note:** `application.yaml` holds absolute local paths for the MCP stdio servers. Change them to match your machine, or remove the `spring.ai.mcp.client.stdio.connections` entries you don't need.

## Authentication

Every `/api/v1/**` endpoint needs a Bearer token from a user with the `PATIENT` or `ADMIN` role. Document ingestion is limited to `ADMIN`.

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8090/realms/medassistant/protocol/openid-connect/token \
  -d grant_type=password \
  -d client_id=medassistant-client \
  -d username=<user> -d password=<password> | jq -r .access_token)
```

## API Reference

Most endpoints accept the same body:

```json
{
  "prompt": "What are the symptoms of hypertension?",
  "model": "openai"
}
```

### Chat — `/api/v1/chat`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Conversational chat with RAG and memory |
| POST | `/stream` | Same as above, streamed over SSE |
| POST | `/explain` | Explain a medical condition in simple terms |
| POST | `/analyze` | Free-text symptom analysis |
| POST | `/diagnose` | Chain-of-thought diagnostic reasoning |
| POST | `/consult` | Role-aware medical consultation |

### Structured analysis — `/api/v1/analysis`

| Method | Path | Returns |
|---|---|---|
| POST | `/condition` | `ConditionSummary` |
| POST | `/condition/v2` | `ConditionSummary` (alternative implementation) |
| POST | `/conditions` | `List<ConditionSummary>` |
| POST | `/symptoms` | `SymptomAnalysis` (possible conditions, severity, urgency) |
| POST | `/classification` | `QueryClassification` |

### Agents — `/api/v1/agent`

| Method | Path | Pattern |
|---|---|---|
| POST | `/chain/appointment` | Prompt chaining: find an appointment from natural language |
| POST | `/chain/appointment/confirm` | Confirm the pending booking (no body) |
| POST | `/chain/appointment/cancel` | Cancel the pending booking (no body) |
| POST | `/chain/routing` | Routing workflow |
| POST | `/chain/orchestrator` | Orchestrator-workers |
| POST | `/chain/orchestrator/stream` | Orchestrator-workers, streamed over SSE |
| POST | `/open` | Autonomous agent with tool calling |

### Documents — `/api/v1/documents` *(ADMIN)*

| Method | Path | Description |
|---|---|---|
| POST | `/ingest` | Upload a file (`multipart/form-data`, field `file`) into the vector store |

```bash
curl -X POST http://localhost:8080/api/v1/documents/ingest \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@src/main/resources/docs/informacion-ibuprofeno.pdf"
```

Sample documents are in `src/main/resources/docs/`.

### MCP client demo — `/api/v1/mcp`

| Method | Path | Description |
|---|---|---|
| POST | `/demo` | Chat that can use tools from the connected MCP servers |

### Example

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "I need a cardiology appointment for tomorrow", "model": "openai"}'
```

## MCP Server

The app exposes a **stateless MCP server** at `http://localhost:8080/mcp`, with no authentication required. It provides:

- **Resources:** doctor directory (`DoctorResourceProvider`)
- **Prompts:** system consultation prompt (`SystemConsultationPromptProvider`)

Requests must include the header `Accept: application/json, text/event-stream`. You can connect any MCP-compatible client to it, such as Claude Desktop, MCP Inspector, or Postman.

## Project Structure

```
src/main/java/com/devtalles/medassistant
├── agent/              # Agentic workflows (chaining, routing, orchestrator-workers, open agent)
├── config/             # ChatClients, vector stores, security, memory, advisors, data loader
├── controller/         # REST endpoints
├── domain/             # JPA entities (Patient, Doctor, Appointment)
├── dto/                # Request/response records & structured-output types
├── mcp/                # MCP server resources & prompts
├── repository/         # Spring Data JPA repositories
├── service/            # Business logic & LLM interactions
├── tools/              # @Tool methods exposed to the LLM
└── util/
src/main/resources
├── prompts/            # StringTemplate (.st) prompt files
├── docs/               # Sample medical PDFs for RAG
└── application*.y(a)ml # Profile configuration
```

## Docker / Production Build

The multi-stage `Dockerfile` builds the jar with Maven and runs it on a slim JRE 21 image. It runs as a non-root user with the `aws` profile turned on.

```bash
docker build -t medassistant .
docker run -p 8080:8080 \
  -e OPEN_AI_API_KEY=sk-... \
  -e RDS_HOST=... -e RDS_DB=... -e RDS_USERNAME=... -e RDS_PASSWORD=... \
  -e KEYCLOAK_ISSUER_URI=https://<keycloak>/realms/medassistant \
  medassistant
```

---

Built as part of the **DevTalles Spring AI** course.
