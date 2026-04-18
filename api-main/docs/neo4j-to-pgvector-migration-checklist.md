# Neo4j to Postgres pgvector Migration Checklist

## Goal

- Remove Neo4j from `api-main`
- Consolidate local development onto PostgreSQL
- Store summary embeddings with `pgvector`
- Preserve current behavior of:
  - knowledge path save
  - partial success nearest-keyword match
  - knowledge tree response shape

## Current Design Summary

- Neo4j is only used in the `tree` domain
- Current similarity logic compares the input embedding against existing summary embeddings and returns the nearest keyword
- The PostgreSQL target design should keep that behavior by storing embeddings on `summaries`

## Target Schema

- `knowledge_categories`
  - `id`
  - `user_id`
  - `name`
  - unique `(user_id, name)`
- `knowledge_topics`
  - `id`
  - `category_id`
  - `name`
  - unique `(category_id, name)`
- `knowledge_keywords`
  - `id`
  - `topic_id`
  - `name`
  - unique `(topic_id, name)`
- `summaries`
  - add `keyword_id`
  - add `embedding vector(1536)`
  - add `embedding_model`

## Execution Checklist

- [x] Define migration target architecture and data model
- [x] Decide to reset local DB instead of migrating existing data
- [x] Replace local infra from MySQL + Neo4j to PostgreSQL + pgvector
- [x] Replace Gradle dependencies for PostgreSQL and remove Neo4j starter
- [x] Introduce managed schema migration with Flyway
- [x] Add relational knowledge entities for category/topic/keyword
- [x] Extend `Summary` with keyword reference and embedding fields
- [x] Implement PostgreSQL repositories for knowledge hierarchy persistence
- [x] Implement pgvector nearest-keyword query
- [x] Rewrite `KnowledgeService` to use PostgreSQL only
- [x] Remove user node bootstrap logic from OAuth flow
- [x] Replace Neo4j repository tests with PostgreSQL tests
- [x] Remove Neo4j config, entities, repository, tests, and compose service
- [x] Run verification for tree save, partial success similarity, and tree read

## Implementation Notes

- Use `pgvector` cosine distance operator `<=>`
- Start with exact nearest-neighbor search
- Avoid approximate indexes until data volume justifies them
- Prefer JPA for hierarchy persistence and native SQL or `JdbcTemplate` for vector search

## Query Shape

```sql
select s.keyword_id
from summaries s
where s.user_id = :user_id
  and s.keyword_id is not null
  and s.embedding is not null
order by s.embedding <=> cast(:embedding as vector)
limit 1;
```

## Validation Checklist

- [x] Project compiles with PostgreSQL-based knowledge implementation
- [x] Summary success flow stores category/topic/keyword and embedding
- [x] Partial success flow links summary to nearest existing keyword with pgvector
- [x] `/api/tree` returns the same `Map<String, Map<String, List<String>>>` shape
- [x] Recommend task payload still contains valid `knowledge_tree`
- [x] No Neo4j dependency remains in build, config, or tests

## Swagger Dev Testing

- `dev` profile에서만 `POST /api/auth/dev-token` 엔드포인트가 활성화된다
- Swagger UI에서 `POST /api/auth/dev-token` 호출 후 `accessToken`을 받는다
- Swagger UI 우측 상단 `Authorize`에 `Bearer <accessToken>` 형식으로 입력한다
- 이후 인증이 필요한 API를 바로 테스트할 수 있다

### Sample Request

```json
{
  "email": "swagger-dev@example.com",
  "name": "Swagger Dev User",
  "careerGoal": "Backend Developer"
}
```

### Suggested Manual Checks

- `POST /api/auth/dev-token`으로 토큰 발급
- `GET /api/tree` 호출
- `POST /api/summary`로 task 생성
- `POST /api/recommend`로 추천 task 생성
