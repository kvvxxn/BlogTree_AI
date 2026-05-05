# BlogTree AI

AI 기반 기술 블로그 요약 및 지식 그래프 시각화 서비스입니다. 사용자가 기술 아티클 URL을 입력하면 핵심 내용을 요약하고 학습 키워드를 추출하여 지식 트리 형태로 시각화합니다. 또한 사용자의 학습 흐름을 고려하여 다음 학습 키워드를 추천합니다.

## 주요 기능

- Google OAuth 기반 소셜 로그인
- 기술 블로그 URL 기반 AI 요약 생성
- 사용자 학습 키워드 기반 추천 콘텐츠 생성
- 작업 진행 상태 실시간 알림
- 사용자별 지식 트리 및 키워드 그래프 조회
- 카테고리, 토픽, 최근 키워드 기반 학습 통계 제공
- OpenAI 임베딩과 pgvector를 활용한 유사 키워드 검색

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | React, TypeScript, Vite, React Router |
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA |
| AI Worker | Python, FastAPI, OpenAI API, BeautifulSoup, Trafilatura |
| Database | PostgreSQL, pgvector, Flyway |
| Messaging | RabbitMQ |
| Auth | Google OAuth 2.0, JWT |
| Infra | Docker, Docker Compose, Caddy, AWS EC2, Vercel |

## 아키텍처

```text
React Frontend
      |
      v
Spring Boot API  <---->  PostgreSQL + pgvector
      |
      v
   RabbitMQ
      |
      v
FastAPI AI Worker  <---->  OpenAI API
```

## 프로젝트 구조

```text
BlogTree_AI/
├── frontend/        # React 클라이언트
├── api-main/        # Spring Boot API 서버
├── fastapi_worker/  # AI 요약/추천 워커
└── deploy/          # 배포 설정
```

## 핵심 구현 포인트

- 요약/추천 작업을 비동기 메시지 큐 기반으로 분리해 API 응답성과 AI 처리 안정성을 개선했습니다.
- SSE를 사용해 장시간 실행되는 AI 작업의 진행/완료/실패 상태를 프론트엔드에 실시간으로 전달했습니다.
- Flyway 기반 DB 마이그레이션과 pgvector 저장소를 구성해 지식 검색 기능을 확장 가능하게 설계했습니다.
- JWT 액세스 토큰과 리프레시 쿠키를 분리해 인증 흐름을 구성했습니다.
- 프론트엔드는 기능 단위 API 클라이언트와 페이지를 분리해 유지보수성을 높였습니다.

## 배포

- Frontend: Vercel
- Backend/Worker/DB/MQ: AWS EC2 + Docker Compose
- Reverse Proxy: Caddy
