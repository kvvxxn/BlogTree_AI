import { Navigate, Outlet, useLocation } from "react-router-dom";
import { saveRedirectAfterLogin } from "@/features/auth/lib/auth-redirect";
import { useAuthSession } from "@/features/auth/providers/AuthSessionProvider";

export function ProtectedRoute() {
  const location = useLocation();
  const { status } = useAuthSession();

  if (status === "checking") {
    return (
      <main className="login-shell">
        <section className="card card--hero callback-card">
          <span className="section-label">Session</span>
          <h1>세션 확인 중</h1>
          <p>저장된 로그인 상태를 복구하고 있습니다.</p>
        </section>
      </main>
    );
  }

  if (status !== "authenticated") {
    saveRedirectAfterLogin(`${location.pathname}${location.search}${location.hash}`);
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
