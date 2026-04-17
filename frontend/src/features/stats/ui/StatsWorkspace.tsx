// 데모 데이터
const totalBlogs = 42;

const categoryData = [
  { name: "Backend", count: 25, percentage: 60, color: "#818cf8" },
  { name: "AI", count: 17, percentage: 40, color: "#2dd4bf" },
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
  { name: "Spring Security", time: "1주 전" },
];

export function StatsWorkspace() {
  // 도넛 차트 계산
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const gap = 4; // 섹션 간 간격
  
  let cumulativeOffset = 0;
  const segments = categoryData.map((cat) => {
    const segmentLength = (cat.percentage / 100) * circumference - gap;
    const offset = cumulativeOffset;
    cumulativeOffset += (cat.percentage / 100) * circumference;
    return {
      ...cat,
      dasharray: `${segmentLength} ${circumference}`,
      dashoffset: -offset,
    };
  });

  return (
    <section className="page-stack">
      {/* 0. 총 학습 블로그 - Hero */}
      <section className="stats-hero card">
        <div className="stats-hero__glow" />
        <div className="stats-hero__content">
          <span className="section-label">Total Knowledge</span>
          <p className="stats-hero__text">
            <span className="stats-hero__number">{totalBlogs}</span>
            개의 블로그를 트리에 연결했어요!
          </p>
        </div>
      </section>

      <section className="content-grid content-grid--wide">
        {/* 1. 나의 학습 밸런스 - Donut Chart */}
        <article className="card">
          <span className="section-label">Learning Balance</span>
          <h2>나의 학습 밸런스</h2>
          
          <div className="stats-donut">
            <div className="stats-donut__chart">
              <svg viewBox="0 0 100 100" className="stats-donut__svg">
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
                  />
                ))}
                {/* 카테고리 라벨 */}
                <text x="85" y="35" className="stats-donut__text" fill="#818cf8" fontSize="6" fontWeight="600">Backend</text>
                <text x="5" y="75" className="stats-donut__text" fill="#2dd4bf" fontSize="6" fontWeight="600">AI</text>
              </svg>
              <div className="stats-donut__center">
                <span className="stats-donut__total">{totalBlogs}</span>
                <span className="stats-donut__label">Total</span>
              </div>
            </div>
            
            <div className="stats-donut__legend">
              {categoryData.map((cat) => (
                <div 
                  key={cat.name} 
                  className="stats-donut__card"
                  style={{ borderColor: cat.color }}
                >
                  <div className="stats-donut__card-header">
                    <span className="stats-donut__dot" style={{ background: cat.color }} />
                    <span className="stats-donut__card-name">{cat.name}</span>
                  </div>
                  <div className="stats-donut__card-stats">
                    <span className="stats-donut__card-percent" style={{ color: cat.color }}>{cat.percentage}%</span>
                    <span className="stats-donut__card-count">{cat.count}개</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          <p className="stats-insight">
            현재 <strong>Backend</strong> 카테고리 관련 블로그를 가장 많이 읽고 있어요!
          </p>
        </article>

        {/* 2. 가장 많이 학습한 토픽 TOP 3 */}
        <article className="card">
          <span className="section-label">Top Topics</span>
          <h2>집중 공략 토픽 TOP 3</h2>
          
          <div className="stats-ranking-simple">
            {topTopics.map((topic) => (
              <div key={topic.rank} className="stats-ranking-simple__item">
                <span className="stats-ranking-simple__medal">
                  {topic.rank === 1 && "🥇"}
                  {topic.rank === 2 && "🥈"}
                  {topic.rank === 3 && "🥉"}
                </span>
                <span className="stats-ranking-simple__name">{topic.name}</span>
                <span className="stats-ranking-simple__count">(블로그 {topic.count}개)</span>
              </div>
            ))}
          </div>
        </article>
      </section>

      {/* 3. 최근 획득한 키워드 */}
      <section className="card">
        <span className="section-label">Recent Keywords</span>
        <h2>최근 획득한 키워드</h2>
        
        <div className="stats-keywords">
          {recentKeywords.map((kw) => (
            <div key={kw.name} className="stats-keyword">
              <span className="stats-keyword__badge">{kw.name}</span>
              <span className="stats-keyword__time">{kw.time}</span>
            </div>
          ))}
        </div>
      </section>
    </section>
  );
}
