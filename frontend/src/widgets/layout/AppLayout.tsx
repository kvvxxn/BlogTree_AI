import { NavLink, Outlet, useLocation } from "react-router-dom";
import { logout } from "@/features/auth/api/auth.api";
import { SummaryPanel } from "@/features/summary/ui/SummaryPanel";

const navItems = [
  { to: "/knowledge-graph", label: "Knowledge Graph" },
  { to: "/summary", label: "Summary Lab" },
  { to: "/recommendation", label: "Career Picks" },
  { to: "/profile", label: "Profile" },
];

export function AppLayout() {
  const location = useLocation();
  const currentPath = location.pathname;

  async function handleLogout() {
    try {
      await logout();
    } finally {
      window.location.href = "/login";
    }
  }

  function renderSidebarPanel() {
    switch (currentPath) {
      case "/":
        return null;
      case "/knowledge-graph":
        return (
          <section className="sidebar__panel">
            <div>
              <span className="sidebar__eyebrow">Knowledge Graph</span>
              <h2 className="sidebar__panel-title">내 블로그 트리</h2>
            </div>
            <p className="sidebar__panel-copy">
              읽어본 블로그 내용이 하나의 지식 트리가 됩니다.
            </p>
            <p className="sidebar__panel-copy sidebar__panel-copy--sub">
              카테고리부터 키워드까지 구조화된 학습 데이터를 한눈에 파악하고, 키워드를 클릭해 요약본과 원본 URL이 담긴 지식 카드를 바로 확인해 보세요.
            </p>
          </section>
        );
      case "/summary":
        return <SummaryPanel />;
      case "/recommendation":
        return (
          <section className="sidebar__panel">
            <div>
              <span className="sidebar__eyebrow">Learning Recommendation</span>
              <h2 className="sidebar__panel-title">맞춤형 학습 추천</h2>
            </div>
            <p className="sidebar__panel-copy">
              현재까지 구축한 블로그 트리와 설정하신 커리어 목표를 분석하여, 다음으로 학습하기 좋은 맞춤형 키워드를 추천합니다.
            </p>
            <button className="button button--primary" type="button">
              추천 키워드 생성
            </button>
          </section>
        );
      case "/profile":
        return (
          <section className="sidebar__panel">
            <div>
              <span className="sidebar__eyebrow">Profile</span>
              <h2 className="sidebar__panel-title">내 프로필</h2>
            </div>
            <p className="sidebar__panel-copy">
              계정 정보와 학습 통계를 확인하고 설정을 변경할 수 있습니다.
            </p>
          </section>
        );
      default:
        return null;
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar__header">
          <div className="sidebar__brand">
            <span className="sidebar__eyebrow">BlogTree AI</span>
            <h1>안녕하세요, 사용자님</h1>
            <p>오늘은 어떤 것을 알아볼까요?</p>
          </div>
          <button className="button button--ghost sidebar__logout" type="button" onClick={handleLogout}>
            Logout
          </button>
        </div>

        <nav className="sidebar__nav" aria-label="Primary">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end
              className={({ isActive }) =>
                isActive ? "sidebar__link sidebar__link--active" : "sidebar__link"
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        {renderSidebarPanel()}
      </aside>

      <div className="content-shell">
        

        <main className="page-container">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
