import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { loginWithGoogle } from "@/features/auth/api/auth.api";
import { consumeRedirectAfterLogin } from "@/features/auth/lib/auth-redirect";
import { setAuthTokens } from "@/shared/api/token-storage";
import { env } from "@/shared/config/env";
import { logger } from "@/shared/lib/logger";

type CallbackState = "loading" | "error";

export function AuthCallback() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<CallbackState>("loading");
  const [message, setMessage] = useState("Google 인가 코드를 처리하고 있습니다.");

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const authorizationCode = searchParams.get("code");
    const oauthError = searchParams.get("error");

    logger.info("auth", "OAuth callback 페이지에 진입했습니다.", {
      hasCode: Boolean(authorizationCode),
      oauthError,
    });

    if (oauthError) {
      logger.warn("auth", "Google OAuth 단계에서 에러가 반환되었습니다.", { oauthError });
      setStatus("error");
      setMessage(`Google 로그인에 실패했습니다: ${oauthError}`);
      return;
    }

    if (!authorizationCode) {
      logger.warn("auth", "OAuth callback에 인가 코드가 없습니다.");
      setStatus("error");
      setMessage("인가 코드가 없습니다. Google 로그인부터 다시 진행하세요.");
      return;
    }

    const confirmedAuthorizationCode = authorizationCode;

    let cancelled = false;

    async function exchangeCode() {
      try {
        logger.info("auth", "백엔드로 인가 코드 교환을 요청합니다.");
        const response = await loginWithGoogle({
          authorizationCode: confirmedAuthorizationCode,
          redirectUri: env.googleRedirectUri,
        });

        if (cancelled) {
          return;
        }

        logger.info("auth", "로그인 성공, 토큰을 저장합니다.");
        setAuthTokens(response.accessToken, response.refreshToken);
        const redirectPath = consumeRedirectAfterLogin();
        logger.info("auth", "로그인 후 이동 경로를 결정했습니다.", { redirectPath });
        navigate(redirectPath, { replace: true });
      } catch (error) {
        if (cancelled) {
          return;
        }

        logger.error("auth", "인가 코드 교환 또는 토큰 저장에 실패했습니다.", error);
        setStatus("error");
        setMessage(
          error instanceof Error
            ? error.message
            : "로그인 처리 중 알 수 없는 오류가 발생했습니다.",
        );
      }
    }

    void exchangeCode();

    return () => {
      logger.debug("auth", "OAuth callback effect를 정리합니다.");
      cancelled = true;
    };
  }, [navigate]);

  return (
    <main className="login-shell">
      <section className="card card--hero callback-card">
        <span className="section-label">Auth Callback</span>
        <h1>{status === "loading" ? "로그인 처리 중" : "로그인 실패"}</h1>
        <p>{message}</p>
        {status === "error" ? (
          <button
            className="button button--primary"
            type="button"
            onClick={() => navigate("/login", { replace: true })}
          >
            로그인 화면으로 이동
          </button>
        ) : null}
      </section>
    </main>
  );
}
