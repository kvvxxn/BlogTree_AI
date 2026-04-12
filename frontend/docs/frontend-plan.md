# Frontend Plan

## Assumption

- 프로젝트는 `React + TypeScript + Vite` 기준으로 시작한다.
- 인증은 JWT access token + `Refresh-Token` 헤더 재발급 흐름을 사용한다.
- 요약/추천은 동기 응답이 아니라 `taskId`를 받고 SSE로 완료 이벤트를 구독한 뒤 상세 조회한다.

## Suggested Structure

```text
frontend/
├─ docs/
│  └─ frontend-plan.md
├─ public/
├─ src/
│  ├─ app/
│  │  ├─ providers/
│  │  ├─ router/
│  │  └─ App.tsx
│  ├─ pages/
│  ├─ widgets/
│  │  └─ layout/
│  ├─ features/
│  │  ├─ auth/
│  │  ├─ user/
│  │  ├─ tree/
│  │  ├─ summary/
│  │  ├─ recommend/
│  │  └─ task/
│  └─ shared/
│     ├─ api/
│     ├─ config/
│     ├─ lib/
│     ├─ types/
│     └─ ui/
├─ index.html
├─ package.json
├─ tsconfig.json
└─ vite.config.ts
```

## API Mapping

- `auth`
  - `POST /api/auth/google`
  - `POST /api/auth/reissue`
  - `POST /api/auth/logout`
- `user`
  - `GET /api/users/me`
  - `PATCH /api/users/me`
- `tree`
  - `GET /api/tree`
- `summary`
  - `POST /api/summary`
  - `GET /api/summary/{summaryId}`
- `recommend`
  - `POST /api/recommend`
  - `GET /api/recommend/{recommendationId}`
- `task`
  - `GET /api/tasks/subscribe/{taskId}`

## Implementation Order

1. 인증 기반부터 고정한다.
   - Google 로그인 콜백 처리
   - access/refresh token 저장
   - 401 시 `reissue` 자동 재시도
   - 보호 라우트 분리
2. 공통 API 레이어를 완성한다.
   - `fetch` wrapper
   - 공통 에러 모델
   - 환경변수(`VITE_API_BASE_URL`)
3. Task/SSE 흐름을 먼저 안정화한다.
   - `POST /api/summary`, `POST /api/recommend`
   - `taskId` 수신
   - SSE 구독
   - terminal event 수신 시 상세 조회
4. 화면 우선순위는 메인 사용 흐름부터 간다.
   - 로그인
   - 대시보드
   - 요약 요청/결과
   - 추천 요청/결과
   - 프로필
5. 마지막에 UI 정리와 예외 처리를 붙인다.
   - 로딩/에러/빈 상태
   - 403/404 메시지 분기
   - 재요청 UX

## Important Note

- 백엔드 SSE 구독 엔드포인트는 인증이 필요하다.
- 브라우저 기본 `EventSource`는 `Authorization` 헤더를 붙일 수 없어서 그대로 쓰기 어렵다.
- 그래서 현재 구조는 `fetch` 스트림 기반 SSE 파서를 두고, 여기서 bearer token을 붙이는 방향으로 잡았다.
