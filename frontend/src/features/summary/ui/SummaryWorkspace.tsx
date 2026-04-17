export function SummaryWorkspace() {
  return (
    <section className="page-stack">
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
      </section>
    </section>
  );
}
