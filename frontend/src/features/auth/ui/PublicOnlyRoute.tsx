import { Navigate, Outlet } from "react-router-dom";
import { useAuthSession } from "@/features/auth/providers/AuthSessionProvider";

export function PublicOnlyRoute() {
  const { status } = useAuthSession();

  if (status === "checking") {
    return (
      <main className="login-shell">
        <section className="card card--hero callback-card">
          <span className="section-label">Session</span>
          <h1>세션 확인 중</h1>
          <p>로그인 상태를 확인하고 있습니다.</p>
        </section>
      </main>
    );
  }

  if (status === "authenticated") {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
