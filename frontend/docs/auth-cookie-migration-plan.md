# Auth Cookie Migration Plan

## Goal

현재 `localStorage` 기반 refresh token 저장 방식을 제거하고, `정적 프론트 + 별도 백엔드 API + HttpOnly refresh cookie` 구조로 전환한다.

## Target Architecture

- 프론트는 계속 정적 배포한다.
- access token은 프론트가 짧게 보관한다.
- refresh token은 백엔드가 `Set-Cookie`로 내려주고, 브라우저가 자동 전송한다.
- 재발급과 로그아웃은 refresh cookie 기준으로 처리한다.

## API Contract Changes

### `POST /api/auth/google`

- 요청: 기존과 동일하게 `authorizationCode`, `redirectUri`
- 응답 바디: `accessToken` 중심으로 단순화
- 응답 쿠키: refresh token을 `HttpOnly` 쿠키로 설정

### `POST /api/auth/reissue`

- 요청 헤더의 `Refresh-Token` 제거
- 브라우저 쿠키에 담긴 refresh token으로 재발급
- 성공 시 새 access token 반환
- 필요하면 refresh token rotation 후 새 cookie 갱신

### `POST /api/auth/logout`

- 서버 저장소의 refresh token 삭제
- refresh cookie 만료 처리
- 프론트는 로컬 access token만 제거

## Backend Tasks

1. `AuthController`에서 `reissue`가 쿠키에서 refresh token을 읽도록 변경
2. `google login` 성공 시 refresh token을 응답 바디가 아니라 `HttpOnly` cookie로 내려주기
3. `logout`에서 refresh cookie를 만료시키는 응답 추가
4. `SecurityConfig` CORS 설정에서 `allowCredentials(true)` 유지
5. 환경별 cookie 옵션 분리
   - local: `Secure=false` 가능
   - production: `Secure=true`
   - cross-origin이면 `SameSite=None`
6. 관련 통합 테스트 추가
   - 로그인 시 `Set-Cookie`
   - 재발급 시 cookie 기반 성공
   - 로그아웃 시 cookie 삭제

## Frontend Tasks

1. `token-storage.ts`에서 refresh token 저장/조회/삭제 제거
2. `http.ts`의 모든 인증 요청에 `credentials: "include"` 추가
3. `reissue` 호출 시 `Refresh-Token` 헤더 제거
4. 로그인 성공 후 access token만 저장
5. 앱 시작 시 access token이 없으면 `reissue` 1회 시도
6. 로그아웃 시 access token 제거 후 `/login` 이동
7. `PublicOnlyRoute`와 `/auth/callback` 가드 구조 점검

## Recommended Access Token Handling

- 1차 목표: access token만 `localStorage`에 남기고 refresh token 제거
- 2차 목표: access token도 메모리 저장으로 전환
- 새로고침 후에는 `reissue`로 access token을 복구

## Environment Notes

```bash
VITE_API_BASE_URL=http://localhost:8080
```

백엔드는 프론트 origin을 정확히 허용해야 하며, 쿠키 전송을 위해 CORS와 cookie 옵션을 함께 맞춰야 한다.

## Migration Order

1. 백엔드가 cookie 기반 refresh 발급/재발급/로그아웃 지원
2. 프론트 `credentials: "include"` 반영
3. 프론트 refresh token localStorage 제거
4. 초기 세션 복구 로직 추가
5. 로그인, 새로고침, 만료, 로그아웃 시나리오 수동 테스트

## Verification Checklist

- 로그인 후 `Set-Cookie`로 refresh token이 내려온다
- `localStorage`에는 refresh token이 남지 않는다
- access token 만료 후 자동 재발급된다
- 새로고침 후 세션이 복구된다
- refresh token 만료 시 `/login`으로 이동한다
- 로그아웃 후 재진입이 차단된다
