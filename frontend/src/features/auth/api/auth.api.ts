import { request } from "@/shared/api/http";
import { clearAuthTokens } from "@/shared/api/token-storage";
import type { LoginRequest, LoginResponse } from "@/shared/types/api";
import { logger } from "@/shared/lib/logger";

export function loginWithGoogle(payload: LoginRequest) {
  logger.debug("auth", "POST /api/auth/google 요청을 전송합니다.", {
    redirectUri: payload.redirectUri,
  });
  return request<LoginResponse>("/api/auth/google", {
    method: "POST",
    body: payload,
    auth: false,
  });
}

export function logout() {
  logger.info("auth", "로그아웃을 요청합니다.");
  return request<string>("/api/auth/logout", {
    method: "POST",
  }).finally(() => {
    logger.info("auth", "로그아웃 후 로컬 토큰을 제거합니다.");
    clearAuthTokens();
  });
}
