# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

MedAssistant: Spring Boot 4.1 / Spring AI 2.0 / Java 21 medical-assistant course project (DevTalles). Code comments, prompts, log/error messages are in Spanish — keep that convention.

## Commands

```bash
docker compose up -d                       # local infra: app Postgres :5432, Keycloak :8090 (+ its Postgres :5433), Chroma :8000
./mvnw spring-boot:run                     # run (default profile → OpenAI + Ollama, Chroma)
./mvnw spring-boot:run -Dspring-boot.run.profiles=aws   # prod-like profile (OpenAI only, PgVector)
./mvnw clean package -DskipTests           # build jar
./mvnw test                                # tests (only a @SpringBootTest contextLoads; needs infra running)
./mvnw test -Dtest=ClassName#method        # single test
docker compose -f docker-compose.aws-test.yml up -d    # pgvector Postgres :5434 for testing the aws profile
```

Required env: `OPEN_AI_API_KEY`; `GITHUB_TOKEN` (GitHub MCP client). Ollama at `localhost:11434` with `llama3.2:1b` and `nomic-embed-text` pulled. `aws` profile also reads `RDS_HOST/PORT/DB/USERNAME/PASSWORD`, `KEYCLOAK_ISSUER_URI`.

`./postgres` and `./keycloak-postgres` are bind-mounted DB data dirs — don't edit/delete.

## Architecture

**Profiles drive bean wiring.** Two parallel config sets selected by `@Profile`:
- `!aws` (default): `AssistantConfig` builds `openAiClient` + `ollamaClient` ChatClients and `openAiVectorStore` + `ollamaVectorStore` (Chroma, separate collections per embedding model). `ClientResolverDefault`, `DocumentIngestionServiceImpl`.
- `aws`: `AssistantConfigAws` (OpenAI only, PgVector via `JdbcTemplate`; `schema-aws.sql` creates the `vector` extension), `ClientResolverAws`, `DocumentIngestionServiceAwsImpl`. Dockerfile hardcodes `SPRING_PROFILES_ACTIVE=aws`.
- Vector store / Ollama autoconfigs are excluded in YAML and beans are created manually — when adding a store/model, wire it explicitly and use `@Qualifier`.

**Per-request model selection.** Requests carry `model` (`"openai"`/`"ollama"`); services call `ClientResolver.resolveClient/resolveModel/resolveVectorStore(model)`. `resolveModel` gives a raw `ChatModel` for building a clean ChatClient without default advisors.

**Default ChatClient advisors:** `QuestionAnswerAdvisor` (RAG over vector store) + `MedicalAuditAdvisor` (logging); chat memory is JDBC-backed (`MemoryConfig`), conversation id = JWT `userId` claim passed via `ChatMemory.CONVERSATION_ID`.

**Prompts** are StringTemplate files in `src/main/resources/prompts/*.st`, injected via `@Value("classpath:prompts/...")`.

**Feature areas (packages under `com.devtalles.medassistant`):**
- `controller/` — REST under `/api/v1/{chat,analysis,agent,documents,mcp}`.
- `service/` — chat, structured-output analysis (`dto/analysis` records/enums), document ingestion (Tika → vector store).
- `agent/` — agentic patterns: prompt chaining (`AppointmentChainService` + `PendingBookingStore` for confirm/cancel), routing (`RoutingWorkflowService`, `routing-*.st`), orchestrator-workers (`agent/orchestrastor/` — note spelling), open agent with tools (`OpenAgentServiceImpl`).
- `tools/` — Spring AI `@Tool` beans over JPA services (patients, doctors, appointments, drugs).
- `mcp/` — this app is also an MCP server (`@McpResource`, `@McpPrompt`), stateless, at `/mcp`. It's also an MCP client (stdio: filesystem, github, engram) configured in `application.yaml` with absolute local paths; disabled in `aws` profile.

**Security:** OAuth2 resource server, Keycloak realm `medassistant`. `SecurityConfig` maps `realm_access.roles` → `ROLE_*`; `/api/v1/**` requires `PATIENT` or `ADMIN`; `/mcp/**` is public. `SecurityUtils.extractRole` used for role-aware prompts.

**Data:** JPA entities `Patient`, `Doctor`, `Appointment`; `DataLoader` seeds sample data on startup. Default profile uses `ddl-auto: create-drop` (data reset each run); `aws` uses `update`. Sample RAG PDFs in `src/main/resources/docs/`.

`TestChatClient` / `TestLlmCall` in the root package are disabled (`//@Component`) demo `CommandLineRunner`s, not tests.
