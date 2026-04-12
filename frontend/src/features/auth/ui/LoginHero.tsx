import { buildGoogleAuthorizeUrl } from "@/features/auth/lib/google-oauth";
import { logger } from "@/shared/lib/logger";

export function LoginHero() {
  function handleGoogleLogin() {
    try {
      const authorizeUrl = buildGoogleAuthorizeUrl();
      logger.info("auth", "Google 로그인 리다이렉트를 시작합니다.", {
        redirectUri: window.location.origin,
      });
      window.location.href = authorizeUrl;
    } catch (error) {
      logger.error("auth", "Google 로그인 URL 생성에 실패했습니다.", error);
      window.alert(
        error instanceof Error
          ? error.message
          : "Google 로그인 URL을 만드는 중 오류가 발생했습니다.",
      );
    }
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
