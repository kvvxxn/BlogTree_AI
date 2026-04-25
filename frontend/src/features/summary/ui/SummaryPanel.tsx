import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSummaryFlow } from "@/features/summary/ui/SummaryFlowProvider";

export function SummaryPanel() {
  const navigate = useNavigate();
  const [url, setUrl] = useState("");
  const [validationMessage, setValidationMessage] = useState("");
  const {
    completionState,
    errorMessage,
    isBusy,
    resetSummary,
    result,
    sourceUrl,
    status,
    streamStatus,
    submitSummary,
    taskId,
  } = useSummaryFlow();

  const isSuccess = status === "success" && result;

  async function handleSubmit() {
    if (!url.trim()) {
      setValidationMessage("URL을 입력해주세요.");
      return;
    }

    const validationError = validateSummaryUrl(url);
    if (validationError) {
      setValidationMessage(validationError);
      return;
    }

    setValidationMessage("");
    await submitSummary(url);
  }

  function handleReset() {
    setUrl("");
    setValidationMessage("");
    resetSummary();
  }

  function handleGoToKnowledgeGraph() {
    navigate("/knowledge-graph");
  }

  const panelErrorMessage = validationMessage || errorMessage;
  const completionMessage = getCompletionMessage(completionState);
  const streamMessage = getStreamMessage(streamStatus);

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

      {!isSuccess && (
        <>
          <label className="field">
            <span>Article URL</span>
            <input
              className="input"
              type="url"
              placeholder="https://example.com/article"
              value={url}
              onChange={(e) => {
                setUrl(e.target.value);
                setValidationMessage("");
              }}
              disabled={isBusy}
            />
          </label>

          <button
            className="button button--primary summary-button"
            type="button"
            onClick={() => void handleSubmit()}
            disabled={isBusy}
          >
            {isBusy ? (
              <>
                <span className="summary-button__spinner" />
                {getSubmitButtonLabel(status, streamStatus)}
              </>
            ) : (
              "요약 요청 보내기"
            )}
          </button>
        </>
      )}

      {status === "processing" && (
        <div className="summary-status summary-status--success">
          <span>{streamMessage}</span>
          {sourceUrl && <span className="summary-status__meta">{sourceUrl}</span>}
          {taskId && <span className="summary-status__meta">Task ID: {taskId}</span>}
        </div>
      )}

      {(validationMessage || status === "error") && panelErrorMessage && (
        <div className="summary-status summary-status--error">
          <span>{panelErrorMessage}</span>
        </div>
      )}

      {isSuccess && result && (
        <>
          {completionMessage && (
            <div className="summary-status summary-status--success">
              <span>{completionMessage}</span>
            </div>
          )}

          <hr className="summary-divider" />
          <div className="summary-result-card">
            <div className="summary-result-card__header">
              <span className="section-label">Knowledge Card</span>
              {renderKnowledgePath(result.category, result.topic, result.keyword)}
            </div>

            <div className="summary-result-card__body">
              <div className="summary-result-card__section">
                <strong>요약본</strong>
                <p>{result.content}</p>
              </div>

              <div className="summary-result-card__section">
                <strong>원본 URL</strong>
                <p>
                  <a href={result.sourceUrl} target="_blank" rel="noreferrer">
                    {result.sourceUrl}
                  </a>
                </p>
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

function validateSummaryUrl(value: string) {
  try {
    const url = new URL(value);
    const supportedDomains = ["tistory.com", "velog.io"];
    const isSupportedBlog = supportedDomains.some((domain) => url.hostname.includes(domain));

    if (!isSupportedBlog) {
      return "현재 Tistory, Velog 블로그의 URL만 지원합니다.";
    }

    return "";
  } catch {
    return "지원하지 않는 URL 형식입니다.";
  }
}

function getSubmitButtonLabel(status: "idle" | "submitting" | "processing" | "success" | "error", streamStatus: "idle" | "connecting" | "live" | "error") {
  if (status === "submitting") {
    return "요약 작업 생성 중...";
  }

  if (status === "processing") {
    if (streamStatus === "connecting") {
      return "실시간 연결 중...";
    }

    return "요약 결과 수신 대기 중...";
  }

  return "요약 요청 보내기";
}

function getStreamMessage(streamStatus: "idle" | "connecting" | "live" | "error") {
  if (streamStatus === "connecting") {
    return "작업은 생성되었습니다. 실시간 결과 스트림에 연결하는 중입니다.";
  }

  if (streamStatus === "live") {
    return "실시간 연결이 열렸습니다. 백엔드 작업 완료 이벤트를 기다리는 중입니다.";
  }

  return "작업 진행 상태를 확인하는 중입니다.";
}

function getCompletionMessage(completionState: "success" | "partial" | null) {
  if (completionState === "partial") {
    return "요약은 완료되었고, 가장 유사한 기존 키워드에 연결되었습니다.";
  }

  if (completionState === "success") {
    return "요약이 완료되어 새로운 지식 경로가 반영되었습니다.";
  }

  return "";
}

function renderKnowledgePath(category: string | null, topic: string | null, keyword: string | null) {
  if (!category || !topic || !keyword) {
    return <p className="summary-path-note">아직 트리 경로가 연결되지 않았습니다.</p>;
  }

  return (
    <h3 className="summary-result-card__title-path">
      <span className="knowledge-title__category">{category}</span>
      <span className="knowledge-title__arrow">→</span>
      <span className="knowledge-title__topic">{topic}</span>
      <span className="knowledge-title__arrow">→</span>
      <span className="knowledge-title__keyword">{keyword}</span>
    </h3>
  );
}
