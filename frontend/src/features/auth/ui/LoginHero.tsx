import { useState } from "react";
import { useLocation } from "react-router-dom";
import { buildGoogleAuthorizeUrl } from "@/features/auth/lib/google-oauth";
import { logger } from "@/shared/lib/logger";

export function LoginHero() {
  const location = useLocation();
  const [isRedirecting, setIsRedirecting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const reason = new URLSearchParams(location.search).get("reason");
  const isSessionExpired = reason === "session-expired";

  function handleGoogleLogin() {
    try {
      const authorizationUrl = buildGoogleAuthorizeUrl();
      logger.info("auth", "Google OAuth 로그인 페이지로 이동합니다.", {
        redirectUri: authorizationUrl,
      });
      setErrorMessage(null);
      setIsRedirecting(true);
      window.location.assign(authorizationUrl);
    } catch (error) {
      logger.error("auth", "Google OAuth 로그인 시작에 실패했습니다.", error);
      setIsRedirecting(false);
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "로그인을 시작하지 못했습니다. 설정을 확인해 주세요.",
      );
    }
  }

  return (
    <main className="login-shell">
      <section className="login-simple card">
        <span className="section-label">BlogTree AI</span>
        <h1>로그인</h1>
        <p>
          {isRedirecting ? "Google 로그인 페이지로 이동하고 있습니다." : "Google 계정으로 시작합니다."}
        </p>
        {isSessionExpired ? (
          <p>세션이 만료되었습니다. 다시 로그인해 주세요.</p>
        ) : null}
        {errorMessage ? <p>{errorMessage}</p> : null}
        <button
          className="button button--primary login-simple__button"
          type="button"
          onClick={handleGoogleLogin}
          disabled={isRedirecting}
        >
          {isRedirecting ? "Redirecting..." : "Google Login"}
        </button>
      </section>
    </main>
  );
}
