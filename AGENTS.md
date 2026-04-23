# Repository Guidelines

## Project Structure & Module Organization
This repository contains three services:

- `api-main/`: Java 17 Spring Boot API. Main code lives in `src/main/java/com/navigator/knowledge`, tests in `src/test/java`, and config/migrations in `src/main/resources`.
- `fastapi_worker/`: Python FastAPI worker for summarization and recommendation jobs. App code lives under `app/`, with MQ consumers in `app/mq` and shared settings in `app/core`.
- `frontend/`: Vite + React + TypeScript client. Application code is in `src/`, with route pages in `src/pages`, features in `src/features`, and shared code in `src/shared`.

## Build, Test, and Development Commands
- `cd frontend && npm run dev`: start the Vite dev server.
- `cd frontend && npm run build`: run `tsc -b` and produce a production bundle.
- `cd api-main && ./gradlew bootRun`: start the Spring API locally.
- `cd api-main && ./gradlew test`: run the JUnit 5 test suite.
- `cd api-main && docker compose up -d`: start PostgreSQL, RabbitMQ, the API, and the worker with local dev settings.
- `python -m uvicorn fastapi_worker.app.main:app --reload`: run the worker from the repository root.

## Coding Style & Naming Conventions
Match the style already used in each service: Java uses 4-space indentation and `UpperCamelCase` classes, Python uses 4-space indentation and `snake_case`, and the frontend uses 2-space indentation with `PascalCase` React components. Keep package/module names lowercase, put backend business logic in `service`, HTTP handlers in `controller` or `presentation`, and colocate frontend API clients under each feature’s `api/` folder. No repo-wide formatter or linter is configured, so keep edits consistent with surrounding files.

## Testing Guidelines
Automated tests currently live in `api-main/src/test/java` and use JUnit 5, Spring Boot Test, and Testcontainers. Name test classes after the target behavior, such as `KnowledgeServiceTest` or `RecommendControllerIntegrationTest`. For frontend and worker changes, add focused tests only if you also introduce a test harness; otherwise, include clear manual verification steps in the PR.

## Commit & Pull Request Guidelines
Recent history follows Conventional Commit prefixes such as `feat:`, `fix:`, and `docs:`. Keep subjects short, imperative, and in English, for example `fix: reconnect rabbitmq in worker`. PRs should summarize behavior changes, list affected services, note new env vars or schema changes, and include screenshots for frontend UI changes.

## Security & Configuration Tips
Keep secrets in local `.env` files, not in Git. Expected values include `OPENAI_API_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, `VITE_GOOGLE_CLIENT_ID`, and worker RabbitMQ or Langfuse settings.
