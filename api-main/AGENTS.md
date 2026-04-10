# Repository Guidelines

## Project Structure & Module Organization
This repository is a Java 17 Spring Boot API built with Gradle. Application code lives under `src/main/java/com/navigator/knowledge`, organized by domain (`auth`, `summary`, `task`, `tree`, `user`) plus shared `global` config, security, exception, and AI integration code. Configuration files are in `src/main/resources`, with `application.yml` as the base profile and `application-dev.yml` for local development. Tests live under `src/test/java` and mirror the main package layout. Local infrastructure definitions are in `docker-compose.yml` and `rabbitmq-definitions.json`.

## Build, Test, and Development Commands
Use the Gradle wrapper so contributors stay on the pinned toolchain.

- `./gradlew bootRun`: starts the API with the active Spring profile.
- `./gradlew test`: runs the JUnit 5 test suite, including Mockito-based unit tests and Spring Boot integration tests.
- `./gradlew clean build`: compiles, tests, and produces a full build artifact.
- `docker compose up -d`: starts local MySQL, Neo4j, and RabbitMQ dependencies used by the dev profile.

## Coding Style & Naming Conventions
Follow standard Java conventions: 4-space indentation, `UpperCamelCase` for classes, `lowerCamelCase` for methods and fields, and lowercase package names. Keep controllers in `presentation` or `controller`, business logic in `service`, persistence in `repository`, and DTOs grouped under `dto`. Prefer descriptive test names ending in `Test`, and keep Spring annotations and constructor/builders consistent with the existing codebase. Lombok is already in use; avoid introducing alternative boilerplate patterns without a reason.

## Testing Guidelines
The project uses JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc, and Neo4j test harness support. Put focused unit tests next to the domain package they cover, and reserve `@SpringBootTest` for integration paths such as controllers and persistence wiring. Name tests after the behavior under test, for example `SummaryControllerIntegrationTest` or `KnowledgeServiceTest`.

## Commit & Pull Request Guidelines
Recent history uses Conventional Commit prefixes such as `feat:`, `refactor:`, and `chore:`. Keep commit subjects short and imperative, for example `feat: add summary task retry handling`. Pull requests should describe the behavioral change, note any config or schema impact, link related issues, and include example requests or responses when API behavior changes.

## Security & Configuration Tips
Do not commit real secrets. `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, and `OPENAI_API_KEY` are loaded from environment variables. Prefer the dev profile and local containers when working on integration features.
