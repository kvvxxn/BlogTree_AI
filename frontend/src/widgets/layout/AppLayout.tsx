import { NavLink, Outlet, useLocation } from "react-router-dom";
import { logout } from "@/features/auth/api/auth.api";
import { SummaryPanel } from "@/features/summary/ui/SummaryPanel";
import { RecommendPanel } from "@/features/recommend/ui/RecommendPanel";

const navItems = [
  { to: "/knowledge-graph", label: "Knowledge Graph" },
  { to: "/summary", label: "Summary Lab" },
  { to: "/stats", label: "Learning Stats" },
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
      case "/stats":
        return (
          <section className="sidebar__panel">
            <div>
              <span className="sidebar__eyebrow">Learning Stats</span>
              <h2 className="sidebar__panel-title">학습 통계</h2>
            </div>
            <p className="sidebar__panel-copy">
              지금까지의 학습 현황을 한눈에 확인하세요.
            </p>
          </section>
        );
      case "/recommendation":
        return <RecommendPanel />;
      case "/profile":
        return (
          <section className="sidebar__panel">
            <div>
              <span className="sidebar__eyebrow">Profile</span>
              <h2 className="sidebar__panel-title">내 프로필</h2>
            </div>
            <p className="sidebar__panel-copy">
              계정 정보를 관리할 수 있습니다.
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
