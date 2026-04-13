# Login Flow Plan

## Goal

Google OAuth 인가 코드를 받아 백엔드 로그인 API에 전달하고, access/refresh token을 저장한 뒤 보호 페이지로 진입한다.

## Backend endpoints

- `POST /api/auth/google`
- `POST /api/auth/reissue`
- `POST /api/auth/logout`

## Frontend implementation order

1. 라우트 추가
   - `/login`
   - `/auth/callback`
2. Google 로그인 시작 함수 작성
   - Google OAuth authorize URL 생성
   - `client_id`, `redirect_uri`, `response_type=code`, `scope` 포함
3. 콜백 페이지 구현
   - URL query에서 `code` 추출
   - 없으면 실패 메시지 처리
   - 있으면 `POST /api/auth/google` 호출
4. 토큰 저장
   - access token: localStorage
   - refresh token: localStorage
   - 저장 후 `/`로 이동
5. 보호 라우트 적용
   - access token 없으면 `/login`으로 리다이렉트
   - 로그인 페이지에서 이미 토큰이 있으면 `/`로 리다이렉트
6. 401 재발급 처리
   - 공통 `request()`에서 `POST /api/auth/reissue`
   - 재발급 성공 시 원래 요청 재시도
   - 실패 시 토큰 제거 후 `/login` 이동
7. 로그아웃 연결
   - `POST /api/auth/logout`
   - 성공/실패와 무관하게 토큰 제거
   - `/login` 이동

## Required environment values

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=your-google-client-id
VITE_GOOGLE_REDIRECT_URI=http://localhost:5173/auth/callback
```

## Screen states to implement

- 로그인 대기 상태
- 콜백 처리 중 로딩 상태
- 로그인 실패 상태
- 토큰 만료 후 재로그인 유도 상태

## Important note

현재 백엔드는 프론트가 Google에서 직접 인가 코드를 받아 넘기는 구조다. 따라서 프론트는 Google SDK 없이도 구현할 수 있고, 먼저 일반 OAuth redirect 방식으로 붙이는 것이 가장 단순하다.
