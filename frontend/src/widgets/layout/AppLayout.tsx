import { NavLink, Outlet } from "react-router-dom";
import { logout } from "@/features/auth/api/auth.api";

const navItems = [
  { to: "/", label: "Knowledge Graph" },
  { to: "/summary", label: "Summary Lab" },
  { to: "/recommendation", label: "Career Picks" },
  { to: "/profile", label: "Profile" },
];

export function AppLayout() {
  async function handleLogout() {
    try {
      await logout();
    } finally {
      window.location.href = "/login";
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <span className="sidebar__eyebrow">BlogTree AI</span>
          <h1>안녕하세요, 사용자님</h1>
          <p>오늘은 어떤 것을 알아볼까요?</p>
        </div>

        <nav className="sidebar__nav" aria-label="Primary">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              className={({ isActive }) =>
                isActive ? "sidebar__link sidebar__link--active" : "sidebar__link"
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <section className="sidebar__panel">
          <div>
            <span className="sidebar__eyebrow">Summary Request</span>
            <h2 className="sidebar__panel-title">요약 요청</h2>
          </div>
          <p className="sidebar__panel-copy">
            URL을 입력하면 블로그 내용을 분석해 지식 트리에 연결합니다.
          </p>
          <label className="field">
            <span>Article URL</span>
            <input
              className="input"
              type="url"
              placeholder="https://example.com/article"
            />
          </label>
          <button className="button button--primary" type="button">
            요약 요청 보내기
          </button>
        </section>

        <section className="sidebar__panel">
          <div>
            <span className="sidebar__eyebrow">Learning Recommendation</span>
            <h2 className="sidebar__panel-title">학습 추천</h2>
          </div>
          <p className="sidebar__panel-copy">
            현재 트리를 기준으로 다음 단계 키워드 추천을 생성합니다.
          </p>
          <button className="button button--primary" type="button">
            추천 키워드 생성
          </button>
        </section>

        <div className="sidebar__note">
          <strong>Current backend flow</strong>
          <span>`POST` 요청 후 `taskId`를 받고 SSE로 완료 이벤트를 구독합니다.</span>
        </div>
      </aside>

      <div className="content-shell">
        <header className="topbar">
          <div>
            <span className="topbar__eyebrow">Workspace</span>
            <h2>Frontend prototype</h2>
          </div>
          <div className="topbar__actions">
            <div className="topbar__badge">React + TypeScript</div>
            <button className="button button--ghost" type="button" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        <main className="page-container">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
