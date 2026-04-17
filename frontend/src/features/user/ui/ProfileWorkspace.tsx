export function ProfileWorkspace() {
  return (
    <section className="page-stack">
      <section className="content-grid content-grid--wide">
        <article className="card">
          <span className="section-label">Account</span>
          <h2>기본 정보</h2>
          <div className="profile-card">
            <div className="avatar">HW</div>
            <div>
              <strong>홍길동</strong>
              <p>hwan@example.com</p>
            </div>
          </div>
        </article>

        <article className="card">
          <span className="section-label">Edit</span>
          <h2>프로필 수정</h2>
          <form className="form-layout">
            <label className="field">
              <span>Name</span>
              <input className="input" type="text" placeholder="이름을 입력하세요" />
            </label>

            <label className="field">
              <span>Career Goal</span>
              <textarea
                className="textarea"
                placeholder="예: 백엔드와 AI를 연결하는 엔지니어가 되고 싶습니다."
                rows={6}
              />
            </label>

            <div className="button-row">
              <button className="button button--primary" type="button">
                Save Profile
              </button>
            </div>
          </form>
        </article>
      </section>
    </section>
  );
}
