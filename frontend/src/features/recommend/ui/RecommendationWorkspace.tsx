const recommendationCards = [
  {
    title: "추천 키워드",
    value: "JPA",
    body: "현재 트리와 커리어 목표를 기준으로 가장 인접한 키워드",
  },
  {
    title: "추천 토픽",
    value: "Spring",
    body: "키워드가 속한 토픽으로 학습 경로를 확장하는 기준점",
  },
  {
    title: "추천 카테고리",
    value: "Backend",
    body: "요약과 연결되는 큰 축. 홈 화면 지식 트리와 바로 연결됩니다.",
  },
];

export function RecommendationWorkspace() {
  return (
    <section className="page-stack">
      <section className="hero-banner card">
        <div>
          <span className="section-label">Recommendation</span>
          <h1>지식 트리를 바탕으로 다음 학습 키워드를 추천받습니다.</h1>
          <p>
            추천 작업은 요청 본문 없이 시작되고, 완료 시 `recommendationId`를 받아
            상세 결과를 조회합니다.
          </p>
        </div>
      </section>

      <section className="content-grid">
        <article className="card">
          <span className="section-label">Request</span>
          <h2>추천 요청</h2>
          <p>
            현재 백엔드는 `POST /api/recommend` 한 번으로 task를 생성합니다.
            프론트에서 별도 폼 입력보다 작업 상태 피드백이 더 중요합니다.
          </p>
          <div className="button-row">
            <button className="button button--primary" type="button">
              Generate Recommendation
            </button>
          </div>
        </article>

        <article className="card">
          <span className="section-label">Task Event</span>
          <h2>완료 이벤트 예시</h2>
          <pre className="code-block">{`{
  "taskId": "recommend-task-demo-001",
  "recommendationId": 3,
  "reason": "JPA 이해도를 먼저 올리는 것이 좋습니다.",
  "category": "Backend",
  "topic": "Spring",
  "keyword": "JPA"
}`}</pre>
        </article>
      </section>

      <section className="stat-grid">
        {recommendationCards.map((card) => (
          <article key={card.title} className="card stat-card">
            <span className="section-label">{card.title}</span>
            <strong>{card.value}</strong>
            <p>{card.body}</p>
          </article>
        ))}
      </section>
    </section>
  );
}
