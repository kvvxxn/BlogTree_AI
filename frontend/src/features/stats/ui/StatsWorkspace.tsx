import "./StatsWorkspace.css";

// 데모 데이터
const totalBlogs = 42;

const categoryData = [
  { name: "Backend", count: 25, percentage: 60, color: "#818cf8" }, // Indigo
  { name: "AI", count: 17, percentage: 40, color: "#2dd4bf" }, // Teal
];

const topTopics = [
  { rank: 1, name: "Spring", count: 12, percentage: 100 },
  { rank: 2, name: "RAG", count: 8, percentage: 67 },
  { rank: 3, name: "Database", count: 5, percentage: 42 },
];

const recentKeywords = [
  { name: "QueryDSL", time: "오늘" },
  { name: "Vector Index", time: "어제" },
  { name: "Reranking", time: "3일 전" },
  { name: "JPA", time: "5일 전" },
];

export function StatsWorkspace() {
  // 도넛 차트 계산
  const radius = 35;
  const circumference = 2 * Math.PI * radius;
  let cumulativeOffset = 0;

  const segments = categoryData.map((cat) => {
    const segmentLength = (cat.percentage / 100) * circumference;
    const offset = cumulativeOffset;
    cumulativeOffset += segmentLength;
    return {
      ...cat,
      dasharray: `${segmentLength} ${circumference}`,
      dashoffset: -offset,
    };
  });

  return (
    <div className="workspace-wrapper">
      <div className="workspace-container">
        
        {/* 헤더 섹션 */}
        <header className="workspace-header">
          <h1 className="header-title">나의 지식 트리</h1>
          <p className="header-subtitle">커리어 목표: AI Backend Engineer</p>
        </header>

        {/* 0. 총 학습 블로그 - Hero */}
        <section className="stats-card hero-card">
          <div className="hero-glow" />
          <div className="hero-content">
            <span className="section-label">Total Knowledge</span>
            <div className="hero-number-group">
              <span className="hero-number">{totalBlogs}</span>
              <span className="hero-text">개의 블로그를 트리에 연결했어요!</span>
            </div>
          </div>
          <div className="hero-icon">🍃</div>
        </section>

        <div className="stats-grid">
          {/* 1. 나의 학습 밸런스 - Donut Chart */}
          <section className="stats-card donut-section">
            <span className="section-label">Learning Balance</span>
            
            <div className="donut-container">
              <svg viewBox="0 0 100 100" className="donut-svg">
                {/* 배경 트랙 */}
                <circle cx="50" cy="50" r={radius} className="donut-track" />
                {/* 데이터 슬라이스 */}
                {segments.map((seg) => (
                  <circle
                    key={seg.name}
                    cx="50"
                    cy="50"
                    r={radius}
                    fill="none"
                    stroke={seg.color}
                    strokeWidth="12"
                    strokeLinecap="round"
                    strokeDasharray={seg.dasharray}
                    strokeDashoffset={seg.dashoffset}
                    transform="rotate(-90 50 50)"
                    className="donut-slice"
                  />
                ))}
                {/* 외부 텍스트 라벨 */}
                <text x="85" y="30" fill="#818cf8" fontSize="7" fontWeight="bold">Backend</text>
                <text x="5" y="80" fill="#2dd4bf" fontSize="7" fontWeight="bold">AI</text>
              </svg>
              
              {/* 도넛 중앙 텍스트 */}
              <div className="donut-center">
                <span className="donut-center-total">{totalBlogs}</span>
                <span className="donut-center-label">Total</span>
              </div>
            </div>

            {/* 깔끔한 범례 (Legend) */}
            <div className="donut-legend">
              {categoryData.map((cat) => (
                <div key={cat.name} className="legend-item">
                  <div className="legend-info">
                    <span className="legend-dot" style={{ backgroundColor: cat.color }} />
                    <span className="legend-name">{cat.name}</span>
                  </div>
                  <div className="legend-stats">
                    <span className="legend-percent" style={{ color: cat.color }}>{cat.percentage}%</span>
                    <span className="legend-count">{cat.count}개</span>
                  </div>
                </div>
              ))}
            </div>
            <p className="donut-insight">
              현재 <strong className="highlight-indigo">Backend</strong> 카테고리 관련 블로그를 가장 많이 읽고 있어요!
            </p>
          </section>

          {/* 2. 가장 많이 학습한 토픽 TOP 3 */}
          <section className="stats-card ranking-section">
            <span className="section-label">Top Topics</span>
            
            <div className="ranking-list">
              {topTopics.map((topic) => (
                <div key={topic.rank} className="ranking-item">
                  <div className="ranking-info">
                    <div className="ranking-name-group">
                      <span className="ranking-medal">
                        {topic.rank === 1 ? "🥇" : topic.rank === 2 ? "🥈" : "🥉"}
                      </span>
                      <span className="ranking-name">{topic.name}</span>
                    </div>
                    <span className="ranking-count">{topic.count}개</span>
                  </div>
                  {/* 프로그레스 바 */}
                  <div className="ranking-progress-track">
                    <div 
                      className="ranking-progress-fill"
                      style={{ width: `${topic.percentage}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        {/* 3. 최근 획득한 키워드 */}
        <section className="stats-card keywords-section">
          <span className="section-label">Recent Keywords</span>
          
          {/* 태그(배지) 형태의 리스트 */}
          <div className="keyword-list">
            {recentKeywords.map((kw) => (
              <div key={kw.name} className="keyword-tag">
                <span className="keyword-hash">#</span>
                <span className="keyword-name">{kw.name}</span>
                <span className="keyword-time">{kw.time}</span>
              </div>
            ))}
          </div>
        </section>

      </div>
    </div>
  );
}
