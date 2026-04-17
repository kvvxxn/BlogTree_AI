import { useNavigate } from "react-router-dom";
import { setAuthTokens } from "@/shared/api/token-storage";
import { logger } from "@/shared/lib/logger";

export function LoginHero() {
  const navigate = useNavigate();

  function handleGoogleLogin() {
    // 임시: 실제 Google OAuth 없이 바로 대시보드로 이동
    logger.info("auth", "임시 로그인 - 대시보드로 바로 이동합니다.");
    setAuthTokens("demo-access-token", "demo-refresh-token");
    navigate("/");
  }

  return (
    <main className="login-shell">
      <section className="login-simple card">
        <span className="section-label">BlogTree AI</span>
        <h1>로그인</h1>
        <p>Google 계정으로 시작합니다.</p>
        <button
          className="button button--primary login-simple__button"
          type="button"
          onClick={handleGoogleLogin}
        >
          Google Login
        </button>
      </section>
    </main>
  );
}
