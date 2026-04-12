export function SummaryWorkspace() {
  return (
    <section className="page-stack">
      <section className="hero-banner card">
        <div>
          <span className="section-label">Summary</span>
          <h1>URL 하나로 요약 작업을 요청하고 결과를 추적합니다.</h1>
          <p>
            `POST /api/summary`로 작업을 생성한 뒤, `taskId` 기준으로 SSE 구독과
            결과 조회를 이어붙이는 화면입니다.
          </p>
        </div>
      </section>

      <section className="content-grid content-grid--wide">
        <article className="card">
          <div className="section-heading">
            <div>
              <span className="section-label">Request</span>
              <h2>요약 요청 폼</h2>
            </div>
          </div>

          <form className="form-layout">
            <label className="field">
              <span>Article URL</span>
              <input
                className="input"
                type="url"
                placeholder="https://example.com/article"
              />
            </label>

            <div className="button-row">
              <button className="button button--primary" type="button">
                Request Summary
              </button>
              <button className="button button--ghost" type="button">
                Clear
              </button>
            </div>
          </form>
        </article>

        <article className="card">
          <span className="section-label">Task State</span>
          <h2>SSE 진행 상태</h2>
          <div className="task-status">
            <div>
              <strong>Status</strong>
              <span>PROCESSING</span>
            </div>
            <div>
              <strong>Task ID</strong>
              <span>summary-task-demo-001</span>
            </div>
            <div>
              <strong>Last event</strong>
              <span>partial_success</span>
            </div>
          </div>
        </article>
      </section>

      <section className="content-grid content-grid--wide">
        <article className="card">
          <span className="section-label">Result</span>
          <h2>요약 결과 미리보기</h2>
          <div className="result-panel">
            <p>
              백엔드 완료 이벤트에는 `summaryId`, `category`, `topic`, `keyword`,
              `summaryContent`가 포함됩니다. 프론트는 이벤트 수신 직후 상세 조회를
              붙여서 최종 화면 상태를 고정하면 됩니다.
            </p>
            <div className="chip-row">
              <span className="chip">Backend</span>
              <span className="chip">Spring</span>
              <span className="chip">JPA</span>
            </div>
          </div>
        </article>

        <article className="card">
          <span className="section-label">API Sequence</span>
          <h2>호출 순서</h2>
          <ol className="timeline">
            <li>`POST /api/summary`</li>
            <li>`GET /api/tasks/subscribe/{'{taskId}'}`</li>
            <li>`GET /api/summary/{'{summaryId}'}`</li>
          </ol>
        </article>
      </section>
    </section>
  );
}
