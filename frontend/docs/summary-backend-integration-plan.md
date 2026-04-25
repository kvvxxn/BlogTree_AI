# Summary Backend Integration Plan

## Goal

- `docs/frontend-plan.md` 기준의 `요청 -> taskId 수신 -> SSE 구독 -> 상세 조회` 흐름으로 `Summary Lab` 페이지를 실제 백엔드와 연결한다.
- 현재 데모 상태인 summary UI를 제거하고, 실제 summary API와 task SSE 이벤트로 결과를 표시한다.
- 새로고침 이후에도 summary 상세를 다시 읽을 수 있는 구조로 맞춘다.

## Confirmed Backend Contract

### `POST /api/summary`

- 요청

```json
{
  "sourceUrl": "https://example.com/article"
}
```

- 응답
  - `202 Accepted`

```json
{
  "taskId": "task-123"
}
```

### `GET /api/tasks/subscribe/{taskId}`

- 인증 필요
- `fetch` 기반 SSE 구독 필요
- 확인된 terminal 이벤트
  - `SUCCESS`
  - `PARTIAL_SUCCESS`
  - `FAILED`
  - `EXPIRED`

### `SUCCESS` / `PARTIAL_SUCCESS` payload

```json
{
  "taskId": "task-123",
  "summaryId": 10,
  "category": "Backend",
  "topic": "Infra",
  "keyword": "Redis",
  "summaryContent": "..."
}
```

### `FAILED` / `EXPIRED` payload

```json
{
  "taskId": "task-123",
  "code": "TASK_EXPIRED",
  "message": "..."
}
```

### `GET /api/summary/{summaryId}`

- 현재 백엔드는 summary 상세에 경로 정보까지 포함해서 반환한다

```json
{
  "summaryId": 10,
  "taskId": "task-123",
  "category": "Backend",
  "topic": "Infra",
  "keyword": "Redis",
  "sourceUrl": "https://example.com/article",
  "content": "...",
  "createdAt": "2026-04-24T12:34:56"
}
```

- keyword가 아직 연결되지 않은 summary는 아래처럼 `category/topic/keyword`가 `null`일 수 있다

```json
{
  "summaryId": 11,
  "taskId": "task-124",
  "category": null,
  "topic": null,
  "keyword": null,
  "sourceUrl": "https://example.com/article-no-path",
  "content": "...",
  "createdAt": "2026-04-24T12:35:10"
}
```

## What Changed

- 이전에는 `GET /api/summary/{summaryId}`에 경로 정보가 없어서 SSE payload에 의존해야 했다.
- 지금은 summary 상세 응답만으로도 `summaryId`, `taskId`, `category`, `topic`, `keyword`, `sourceUrl`, `content`, `createdAt`를 복원할 수 있다.
- 따라서 프론트는 SSE를 "완료 감지" 용도로 쓰고, 최종 렌더링 데이터는 `getSummary(summaryId)` 응답을 기준으로 맞추는 것이 가장 단순하다.

## Current Frontend Gaps

### 1. 프론트 타입이 백엔드 응답과 맞지 않는다

- `src/shared/types/api.ts`의 현재 `SummaryResponse`는 아래 3개 필드만 가진다
  - `sourceUrl`
  - `content`
  - `createdAt`
- 실제 백엔드는 다음 필드를 추가로 내려준다
  - `summaryId`
  - `taskId`
  - `category`
  - `topic`
  - `keyword`

### 2. Summary UI는 아직 데모 상태다

- `src/features/summary/ui/SummaryPanel.tsx`는 `delay()`와 하드코딩된 결과를 사용한다.
- `src/features/summary/ui/SummaryWorkspace.tsx`는 비어 있다.

### 3. SSE는 아직 운영 보완이 부족하다

- `src/shared/api/sse.ts`는 인증 헤더는 붙이지만, 401 재발급 재시도와 unsubscribe 정리가 없다.

## Recommended Frontend Flow

1. 사용자가 URL을 입력한다.
2. `POST /api/summary`를 호출한다.
3. 응답의 `taskId`를 저장하고 processing 상태로 전환한다.
4. `GET /api/tasks/subscribe/{taskId}`를 구독한다.
5. `SUCCESS` 또는 `PARTIAL_SUCCESS` 이벤트를 받으면 payload의 `summaryId`로 `GET /api/summary/{summaryId}`를 호출한다.
6. 최종 화면은 `GET /api/summary/{summaryId}` 응답 기준으로 렌더링한다.
7. `FAILED` 또는 `EXPIRED` 이벤트를 받으면 에러 상태로 전환한다.

## Frontend Tasks

### 1. API 타입을 새 계약에 맞춘다

- `src/shared/types/api.ts`
  - `SummaryResponse`를 아래 구조로 수정

```ts
export type SummaryResponse = {
  summaryId: number;
  taskId: string;
  category: string | null;
  topic: string | null;
  keyword: string | null;
  sourceUrl: string;
  content: string;
  createdAt: string;
};
```

- 추가 필요 타입
  - `SummaryTaskPartialSuccessEvent`
  - `TaskExpiredEvent`
- `SummaryTaskSuccessEvent`의 `category/topic/keyword`도 nullable로 볼지 검토
  - 현재 백엔드 SSE success/partial success는 값이 채워지는 방향이지만, 프론트는 방어적으로 처리하는 편이 안전하다

### 2. SummaryPanel을 실제 요청 패널로 바꾼다

- `src/features/summary/ui/SummaryPanel.tsx`
  - `delay()` 제거
  - 데모 결과 제거
  - URL 검증은 유지
  - 제출 시 `requestSummary({ sourceUrl: url })` 호출
  - `taskId` 저장 후 processing 상태 전환

- 권장 상태 모델

```ts
type SummaryUiStatus =
  | "idle"
  | "submitting"
  | "processing"
  | "success"
  | "error";
```

### 3. SSE를 완료 감지 레이어로 연결한다

- `taskId`를 받은 직후 `subscribeTask(taskId, onEvent)` 호출
- 이벤트 처리 규칙
  - `SUCCESS`: `getSummary(summaryId)` 호출
  - `PARTIAL_SUCCESS`: `getSummary(summaryId)` 호출 후 별도 안내 문구 표시 가능
  - `FAILED`: 에러 상태 전환
  - `EXPIRED`: 에러 상태 전환

- 요점:
  - 최종 렌더링은 SSE payload가 아니라 `GET /api/summary/{summaryId}` 응답을 기준으로 한다
  - 이렇게 하면 새로고침 후에도 동일한 데이터 구조로 다시 읽을 수 있다

### 4. SummaryWorkspace를 실제 결과 화면으로 채운다

- `src/features/summary/ui/SummaryWorkspace.tsx`
  - 빈 화면 대신 summary 상태를 보여주는 본문으로 변경
  - panel과 상태를 공유하기 위해 custom hook 또는 상위 state lift 필요

- 최소 노출 정보
  - 현재 요청 URL
  - 진행 상태
  - 경로 정보
  - summary 본문
  - 생성 시각
  - `Knowledge Graph 확인` CTA

- 경로 렌더링 규칙
  - `category/topic/keyword`가 모두 있으면 현재 카드 UI 유지
  - 일부 또는 전부 `null`이면 경로 영역을 숨기거나 `아직 트리 경로가 연결되지 않았습니다` 같은 문구로 대체

### 5. SSE 유틸을 보완한다

- `src/shared/api/sse.ts`
  - `AbortController` 또는 unsubscribe 지원
  - JSON parse 실패 방어
  - component unmount 시 reader 정리
  - 401 발생 시 `reissueAccessToken()` 후 1회 재시도 검토

- 이 부분은 아직 백엔드 summary 계약 변경과 무관하게 필요한 안정화 항목이다.

## Simplified Data Strategy

- processing 중에는 아래 상태만 유지하면 된다
  - `taskId`
  - `sourceUrl`
  - `status`
  - `errorMessage`

- success 이후에는 `SummaryResponse` 하나를 화면의 단일 truth source로 둔다
- 즉, 이전 계획처럼 `SSE event + detail response`를 오래 병합해 들고 갈 필요가 줄었다
- SSE payload는 `summaryId` 확보와 즉시 상태 전환 정도에만 사용하면 충분하다

## Implementation Order

1. `src/shared/types/api.ts`의 summary 관련 타입을 새 계약에 맞게 수정한다
2. `SummaryPanel`의 데모 로직을 `POST /api/summary` 호출로 교체한다
3. `subscribeTask()`를 summary 플로우에 연결한다
4. success/partial_success 이벤트에서 `getSummary(summaryId)`를 호출한다
5. `SummaryWorkspace`를 실제 결과 화면으로 채운다
6. failed/expired/session-expired 메시지를 정리한다
7. 새로고침 후 summary 상세 재조회 UX를 점검한다

## Manual Verification Checklist

- 유효한 URL 입력 시 `POST /api/summary`가 `202`와 `taskId`를 반환한다
- 요청 직후 UI가 processing 상태로 전환된다
- `SUCCESS` 또는 `PARTIAL_SUCCESS` 이벤트 수신 후 `GET /api/summary/{summaryId}`가 호출된다
- 결과 화면에 `summaryId`, `taskId`, `sourceUrl`, `content`, `createdAt`가 정상 반영된다
- 경로가 있는 summary는 `category/topic/keyword`가 표시된다
- 경로가 없는 summary는 `category/topic/keyword`가 `null`이어도 화면이 깨지지 않는다
- `FAILED` 이벤트 수신 시 인라인 에러가 노출된다
- `EXPIRED` 이벤트 수신 시 재시도 가능한 안내가 노출된다
- access token 만료 시 HTTP 요청 또는 SSE 구독 실패가 명확히 처리된다

## Definition Of Done

- summary page에서 데모 지연 로직이 제거되어 있다
- summary page가 실제 backend summary endpoint와 SSE를 사용한다
- summary 상세 응답의 새 필드가 프론트 타입과 UI에 반영되어 있다
- 경로 정보가 있는 경우와 없는 경우 모두 정상 렌더링된다
- 실패, 만료, 세션 만료 상태에 대한 사용자 메시지가 존재한다
