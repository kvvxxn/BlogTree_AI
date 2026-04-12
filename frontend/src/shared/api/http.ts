import { env } from "@/shared/config/env";
import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens } from "@/shared/api/token-storage";
import type { ApiErrorResponse, LoginResponse } from "@/shared/types/api";
import { saveRedirectAfterLogin } from "@/features/auth/lib/auth-redirect";
import { logger } from "@/shared/lib/logger";

type HttpMethod = "GET" | "POST" | "PATCH" | "PUT" | "DELETE";

type RequestOptions = {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
  auth?: boolean;
};

export class ApiError extends Error {
  status: number;
  code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (response.ok) {
    if (response.status === 204) {
      return undefined as T;
    }

    return (await response.json()) as T;
  }

  const error = (await response.json().catch(() => null)) as ApiErrorResponse | null;
  throw new ApiError(error?.message ?? "요청 처리 중 오류가 발생했습니다.", response.status, error?.code);
}

async function reissueTokens() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    logger.warn("auth", "refresh token이 없어 재발급을 진행할 수 없습니다.");
    clearAuthTokens();
    return false;
  }

  logger.info("auth", "access token 만료로 재발급을 시도합니다.");
  const response = await fetch(`${env.apiBaseUrl}/api/auth/reissue`, {
    method: "POST",
    headers: {
      "Refresh-Token": refreshToken,
    },
  });

  if (!response.ok) {
    logger.warn("auth", "토큰 재발급에 실패했습니다.", { status: response.status });
    clearAuthTokens();
    return false;
  }

  const data = (await response.json()) as LoginResponse;
  logger.info("auth", "토큰 재발급에 성공했습니다.");
  setAuthTokens(data.accessToken, data.refreshToken);
  return true;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, headers = {}, auth = true } = options;
  const accessToken = auth ? getAccessToken() : null;

  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (response.status !== 401 || !auth) {
    return parseResponse<T>(response);
  }

  logger.warn("auth", "인증 요청이 401을 반환해 토큰 재발급을 시작합니다.", { path });
  const reissued = await reissueTokens();
  if (!reissued) {
    clearAuthTokens();
    if (window.location.pathname !== "/login") {
      logger.warn("auth", "재발급 실패로 로그인 페이지로 이동합니다.");
      saveRedirectAfterLogin(
        `${window.location.pathname}${window.location.search}${window.location.hash}`,
      );
      window.location.href = "/login";
    }
    throw new ApiError("인증이 만료되었습니다.", 401, "UNAUTHORIZED");
  }

  const retriedAccessToken = getAccessToken();
  logger.info("auth", "재발급 후 원래 요청을 다시 시도합니다.", { path });
  const retriedResponse = await fetch(`${env.apiBaseUrl}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(retriedAccessToken ? { Authorization: `Bearer ${retriedAccessToken}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  return parseResponse<T>(retriedResponse);
}
