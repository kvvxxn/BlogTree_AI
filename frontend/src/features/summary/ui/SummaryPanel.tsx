import { useState } from "react";
import { useNavigate } from "react-router-dom";

type SummaryStatus = "idle" | "collecting" | "analyzing" | "mapping" | "success" | "error";

type SummaryResult = {
  title: string;
  summary: string;
  sourceUrl: string;
  category: string;
  topic: string;
  keyword: string;
};

const STATUS_MESSAGES: Record<SummaryStatus, string> = {
  idle: "",
  collecting: "URL 수집 중...",
  analyzing: "블로그 내용 분석 중...",
  mapping: "트리에 매핑 중...",
  success: "요약이 완료되었습니다!",
  error: "",
};

export function SummaryPanel() {
  const navigate = useNavigate();
  const [url, setUrl] = useState("");
  const [status, setStatus] = useState<SummaryStatus>("idle");
  const [errorMessage, setErrorMessage] = useState("");
  const [result, setResult] = useState<SummaryResult | null>(null);

  const isProcessing = status === "collecting" || status === "analyzing" || status === "mapping";

  async function handleSubmit() {
    if (!url.trim()) {
      setStatus("error");
      setErrorMessage("URL을 입력해주세요.");
      return;
    }

    // URL 형식 검증
    try {
      new URL(url);
    } catch {
      setStatus("error");
      setErrorMessage("지원하지 않는 URL 형식입니다.");
      return;
    }

    // 지원 블로그 검증 (Tistory, Velog)
    const supportedDomains = ["tistory.com", "velog.io"];
    const urlObj = new URL(url);
    const isSupportedBlog = supportedDomains.some((domain) => urlObj.hostname.includes(domain));
    
    if (!isSupportedBlog) {
      setStatus("error");
      setErrorMessage("현재 Tistory, Velog 블로그의 URL만 지원합니다.");
      return;
    }

    setErrorMessage("");
    setResult(null);

    // 단계별 진행 시뮬레이션 (실제 구현 시 SSE 이벤트로 대체)
    try {
      setStatus("collecting");
      await delay(1500);

      setStatus("analyzing");
      await delay(2000);

      setStatus("mapping");
      await delay(1500);

      // 성공 시 결과 설정 (데모 데이터)
      setResult({
        title: "Vector Index 최적화 전략",
        summary: "RAG 시스템에서 임베딩 기반 검색 품질과 성능을 좌우하는 인덱싱 전략에 대한 핵심 개념을 다룹니다. HNSW, IVF 등 다양한 인덱싱 방법의 장단점을 비교합니다.",
        sourceUrl: url,
        category: "AI",
        topic: "RAG",
        keyword: "Vector Index",
      });
      setStatus("success");
    } catch {
      setStatus("error");
      setErrorMessage("블로그 내용을 가져올 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
  }

  function handleReset() {
    setUrl("");
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
        <span className="sidebar__eyebrow">Summary Request</span>
        <h2 className="sidebar__panel-title">요약 요청</h2>
      </div>
      <p className="sidebar__panel-copy">
        URL 입력 한 번으로 블로그의 문맥을 분석하고, 요약된 인사이트를 블로그 트리에 매핑합니다.
      </p>
      <p className="sidebar__panel-copy sidebar__panel-copy--sub">
        * 현재 Tistory, Velog 블로그의 URL 입력을 권장합니다.
      </p>

      {status !== "success" && (
        <>
          <label className="field">
            <span>Article URL</span>
            <input
              className="input"
              type="url"
              placeholder="https://example.com/article"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              disabled={isProcessing}
            />
          </label>

          <button
            className="button button--primary summary-button"
            type="button"
            onClick={handleSubmit}
            disabled={isProcessing}
          >
            {isProcessing ? (
              <>
                <span className="summary-button__spinner" />
                {STATUS_MESSAGES[status]}
              </>
            ) : (
              "요약 요청 보내기"
            )}
          </button>
        </>
      )}

      {status === "error" && errorMessage && (
        <div className="summary-status summary-status--error">
          <span>{errorMessage}</span>
        </div>
      )}

      {status === "success" && result && (
        <>
          <div className="summary-result-card">
            <div className="summary-result-card__header">
              <span className="section-label">Knowledge Card</span>
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
                <strong>요약본</strong>
                <p>{result.summary}</p>
              </div>

              <div className="summary-result-card__section">
                <strong>원본 URL</strong>
                <a href={result.sourceUrl} target="_blank" rel="noreferrer">
                  {result.sourceUrl}
                </a>
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
              새 요약
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
