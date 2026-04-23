# Repository Guidelines

## Project Structure & Module Organization
This repository has three services:

- `frontend/`: Vite + React + TypeScript UI. App code lives in `src/`, with pages in `src/pages`, feature modules in `src/features`, and shared utilities in `src/shared`.
- `api-main/`: Java 17 Spring Boot API. Main sources are under `src/main/java/com/navigator/knowledge`, tests under `src/test/java`, and config or migrations under `src/main/resources`.
- `fastapi_worker/`: Python FastAPI worker. Application code is in `app/`, message queue consumers in `app/mq`, and shared settings in `app/core`.

Keep new code inside the service that owns the behavior.

## Build, Test, and Development Commands
- `cd frontend && npm run dev`: start the Vite development server.
- `cd frontend && npm run build`: type-check with `tsc -b` and build the production bundle.
- `cd api-main && ./gradlew bootRun`: run the Spring Boot API locally.
- `cd api-main && ./gradlew test`: execute the JUnit 5 test suite.
- `cd api-main && docker compose up -d`: start PostgreSQL, RabbitMQ, the API, and the worker for local development.
- `python -m uvicorn fastapi_worker.app.main:app --reload`: run the worker from the repository root.

## Coding Style & Naming Conventions
Follow the existing style in each service. Java uses 4-space indentation and `UpperCamelCase` class names. Python uses 4-space indentation and `snake_case`. Frontend code uses 2-space indentation and `PascalCase` React component names. Keep package and module names lowercase. Place backend business logic in `service`, HTTP handlers in `controller` or `presentation`, and frontend API code in each feature’s `api/` folder. No repo-wide formatter is enforced, so match surrounding code.

## Testing Guidelines
Backend tests use JUnit 5, Spring Boot Test, and Testcontainers in `api-main/src/test/java`. Name tests after the behavior under test, for example `KnowledgeServiceTest` or `RecommendControllerIntegrationTest`. Run them with `cd api-main && ./gradlew test`. For frontend and worker changes, add focused tests only if a harness exists; otherwise, document manual verification in the PR.

## Commit & Pull Request Guidelines
Use Conventional Commit prefixes such as `feat:`, `fix:`, and `docs:` with short imperative subjects, for example `fix: reconnect rabbitmq in worker`. Pull requests should summarize the change, list affected services, note new environment variables or schema changes, and include screenshots for frontend UI updates.

## Security & Configuration Tips
Store secrets in local `.env` files, not in Git. Common values include `OPENAI_API_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, `VITE_GOOGLE_CLIENT_ID`, and RabbitMQ or Langfuse settings for the worker.
