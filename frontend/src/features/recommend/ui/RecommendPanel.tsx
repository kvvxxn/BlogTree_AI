import { useState } from "react";
import { useNavigate } from "react-router-dom";

type RecommendStatus = "idle" | "analyzing" | "generating" | "success" | "error";

type RecommendResult = {
  category: string;
  topic: string;
  keyword: string;
  reason: string;
  searchTerms: string[];
};

const STATUS_MESSAGES: Record<RecommendStatus, string> = {
  idle: "",
  analyzing: "블로그 트리 분석 중...",
  generating: "맞춤형 키워드 생성 중...",
  success: "추천이 완료되었습니다!",
  error: "",
};

export function RecommendPanel() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<RecommendStatus>("idle");
  const [errorMessage, setErrorMessage] = useState("");
  const [result, setResult] = useState<RecommendResult | null>(null);

  const isProcessing = status === "analyzing" || status === "generating";

  async function handleGenerate() {
    setErrorMessage("");
    setResult(null);

    try {
      setStatus("analyzing");
      await delay(2000);

      setStatus("generating");
      await delay(2000);

      // 성공 시 결과 설정 (데모 데이터)
      setResult({
        category: "Backend",
        topic: "Spring",
        keyword: "Spring Security",
        reason: "현재 JPA와 QueryDSL을 학습하셨으니, 다음 단계로 인증/인가 처리를 위한 Spring Security를 학습하시면 백엔드 개발 역량을 더욱 강화할 수 있습니다. 특히 JWT 기반 인증과 OAuth2 연동은 실무에서 자주 사용됩니다.",
        searchTerms: [
          "Spring Security 기초",
          "Spring Security JWT 인증",
          "Spring Boot OAuth2 소셜 로그인",
        ],
      });
      setStatus("success");
    } catch {
      setStatus("error");
      setErrorMessage("추천을 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
  }

  function handleReset() {
    setStatus("idle");
    setErrorMessage("");
    setResult(null);
  }

  function handleGoToKnowledgeGraph() {
    navigate("/knowledge-graph");
  }

  return (
    <section className="sidebar__panel">
      <div>
        <span className="sidebar__eyebrow">Learning Recommendation</span>
        <h2 className="sidebar__panel-title">맞춤형 학습 추천</h2>
      </div>
      <p className="sidebar__panel-copy">
        현재까지 구축한 블로그 트리와 설정하신 커리어 목표를 분석하여, 다음으로 학습하기 좋은 맞춤형 키워드를 추천합니다.
      </p>

      {status !== "success" && (
        <button
          className="button button--primary summary-button"
          type="button"
          onClick={handleGenerate}
          disabled={isProcessing}
        >
          {isProcessing ? (
            <>
              <span className="summary-button__spinner" />
              {STATUS_MESSAGES[status]}
            </>
          ) : (
            "추천 키워드 생성"
          )}
        </button>
      )}

      {status === "error" && errorMessage && (
        <div className="summary-status summary-status--error">
          <span>{errorMessage}</span>
        </div>
      )}

      {status === "success" && result && (
        <>
          <hr className="summary-divider" />
          <div className="summary-result-card">
            <div className="summary-result-card__header">
              <span className="section-label">Recommend Card</span>
              <h3 className="summary-result-card__title-path">
                <span className="knowledge-title__category">{result.category}</span>
                <span className="knowledge-title__arrow">→</span>
                <span className="knowledge-title__topic">{result.topic}</span>
                <span className="knowledge-title__arrow">→</span>
                <span className="knowledge-title__keyword">{result.keyword}</span>
              </h3>
            </div>

            <div className="summary-result-card__body">
              <div className="summary-result-card__section">
                <strong>추천 이유</strong>
                <p>{result.reason}</p>
              </div>

              <div className="summary-result-card__section">
                <strong>추천 검색어</strong>
                <ul className="recommend-blog-list">
                  {result.searchTerms.map((term, index) => (
                    <li key={index}>
                      <span>{term}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>

          <div className="summary-result-actions">
            <button
              className="button button--primary"
              type="button"
              onClick={handleGoToKnowledgeGraph}
            >
              Knowledge Graph 확인
            </button>

            <button
              className="button button--ghost"
              type="button"
              onClick={handleReset}
            >
              새 추천
            </button>
          </div>
        </>
      )}
    </section>
  );
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
