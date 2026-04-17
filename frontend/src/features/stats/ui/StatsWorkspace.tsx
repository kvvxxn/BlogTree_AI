// 데모 데이터
const totalBlogs = 42;

const categoryData = [
  { name: "Backend", count: 25, percentage: 60, color: "#f472b6" },
  { name: "AI", count: 17, percentage: 40, color: "#60a5fa" },
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
  return (
    <section className="page-stack">
      {/* 0. 총 학습 블로그 */}
      <section className="stats-hero card">
        <div className="stats-hero__content">
          <span className="section-label">Total Knowledge</span>
          <div className="stats-hero__number">{totalBlogs}</div>
          <p className="stats-hero__desc">개의 블로그를 트리에 연결했어요!</p>
        </div>
      </section>

      <section className="content-grid content-grid--wide">
        {/* 1. 나의 학습 밸런스 */}
        <article className="card">
          <span className="section-label">Learning Balance</span>
          <h2>나의 학습 밸런스</h2>
          
          <div className="stats-pie">
            <div className="stats-pie__chart">
              <svg viewBox="0 0 100 100" className="stats-pie__svg">
                <circle
                  cx="50"
                  cy="50"
                  r="40"
                  fill="none"
                  stroke="#f472b6"
                  strokeWidth="20"
                  strokeDasharray={`${60 * 2.51} ${100 * 2.51}`}
                  strokeDashoffset="0"
                  transform="rotate(-90 50 50)"
                />
                <circle
                  cx="50"
                  cy="50"
                  r="40"
                  fill="none"
                  stroke="#60a5fa"
                  strokeWidth="20"
                  strokeDasharray={`${40 * 2.51} ${100 * 2.51}`}
                  strokeDashoffset={`${-60 * 2.51}`}
                  transform="rotate(-90 50 50)"
                />
              </svg>
            </div>
            <div className="stats-pie__legend">
              {categoryData.map((cat) => (
                <div key={cat.name} className="stats-pie__item">
                  <span
                    className="stats-pie__dot"
                    style={{ background: cat.color }}
                  />
                  <span className="stats-pie__label">{cat.name}</span>
                  <span className="stats-pie__value">{cat.percentage}% ({cat.count}개)</span>
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
          <h2>집중 공략 주제 TOP 3</h2>
          
          <div className="stats-ranking">
            {topTopics.map((topic) => (
              <div key={topic.rank} className="stats-ranking__item">
                <div className="stats-ranking__medal">
                  {topic.rank === 1 && "🥇"}
                  {topic.rank === 2 && "🥈"}
                  {topic.rank === 3 && "🥉"}
                </div>
                <div className="stats-ranking__info">
                  <div className="stats-ranking__header">
                    <span className="stats-ranking__name">{topic.name}</span>
                    <span className="stats-ranking__count">블로그 {topic.count}개</span>
                  </div>
                  <div className="stats-ranking__bar">
                    <div
                      className="stats-ranking__fill"
                      style={{ width: `${topic.percentage}%` }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </article>
      </section>

      {/* 3. 최근 획득한 키워드 */}
      <section className="card">
        <span className="section-label">Recent Keywords</span>
        <h2>최근 획득한 키워드</h2>
        
        <div className="stats-recent">
          {recentKeywords.map((kw) => (
            <div key={kw.name} className="stats-recent__chip">
              <span className="stats-recent__name">#{kw.name}</span>
              <span className="stats-recent__time">{kw.time}</span>
            </div>
          ))}
        </div>
      </section>
    </section>
  );
}
