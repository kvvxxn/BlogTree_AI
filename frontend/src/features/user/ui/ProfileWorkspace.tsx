export function ProfileWorkspace() {
  return (
    <section className="page-stack">
      <section className="content-grid content-grid--wide">
        <article className="card">
          <span className="section-label">Account</span>
          <h2>기본 정보</h2>
          <div className="profile-card">
            <div className="avatar">HW</div>
            <div className="profile-info">
              <strong>홍길동</strong>
              <span className="profile-goal">Backend</span>
              <p>hwan@example.com</p>
            </div>
          </div>
        </article>

        <article className="card">
          <span className="section-label">Edit Name</span>
          <h2>이름 수정</h2>
          <form className="form-layout">
            <input className="input" type="text" placeholder="이름을 입력하세요" />

            <div className="button-row">
              <button className="button button--primary" type="button">
                Save
              </button>
            </div>
          </form>
        </article>

        <article className="card">
          <span className="section-label">Edit Career Goal</span>
          <h2>커리어 목표 수정</h2>
          <form className="form-layout">
            <input
              className="input"
              type="text"
              placeholder="예: Backend Developer"
            />

            <div className="button-row">
              <button className="button button--primary" type="button">
                Save
              </button>
            </div>
          </form>
        </article>
      </section>
    </section>
  );
}
