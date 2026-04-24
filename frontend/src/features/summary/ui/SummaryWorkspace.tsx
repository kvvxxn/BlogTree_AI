import { useNavigate } from "react-router-dom";
import { useSummaryFlow } from "@/features/summary/ui/SummaryFlowProvider";

export function SummaryWorkspace() {
  const navigate = useNavigate();
  const {
    completionState,
    errorMessage,
    result,
    sourceUrl,
    status,
    streamStatus,
    taskId,
  } = useSummaryFlow();

  return (
    <section className="page-stack summary-workspace">
      <article className="summary-workspace__hero">
        <span className="sidebar__eyebrow">Summary Lab</span>
        <h2 className="summary-workspace__title">블로그 요약과 지식 경로를 한 화면에서 확인하세요.</h2>
        <p className="summary-workspace__copy">
          summary page는 URL 요청부터 SSE 완료 이벤트, 최종 summary 상세 조회까지 한 흐름으로 연결됩니다.
        </p>
      </article>

      {status === "idle" && (
        <div className="summary-workspace__grid">
          <article className="summary-workspace__card">
            <span className="section-label">How It Works</span>
            <h3>현재 연결 흐름</h3>
            <p>
              URL을 제출하면 백엔드가 `taskId`를 반환하고, 프론트는 해당 task의 SSE 이벤트를 구독한 뒤
              완료 시 summary 상세를 조회합니다.
            </p>
          </article>

          <article className="summary-workspace__card">
            <span className="section-label">Ready State</span>
            <h3>요약 요청 대기 중</h3>
            <p>
              왼쪽 패널에서 블로그 URL을 입력하면 이 영역에 진행 상태와 최종 요약 결과가 표시됩니다.
            </p>
          </article>
        </div>
      )}

      {(status === "submitting" || status === "processing") && (
        <div className="summary-workspace__grid">
          <article className="summary-workspace__card">
            <span className="section-label">Current Task</span>
            <h3>백엔드 작업 진행 중</h3>
            <div className="summary-workspace__meta">
              <span>상태: {getProcessingStatusLabel(status, streamStatus)}</span>
              {taskId && <span>Task ID: {taskId}</span>}
              {sourceUrl && <span>URL: {sourceUrl}</span>}
            </div>
          </article>

          <article className="summary-workspace__card">
            <span className="section-label">Next Step</span>
            <h3>완료 이벤트를 기다리는 중</h3>
            <p>
              `SUCCESS` 또는 `PARTIAL_SUCCESS` 이벤트를 받으면 `GET /api/summary/{'{summaryId}'}`로
              최종 결과를 조회합니다.
            </p>
          </article>
        </div>
      )}

      {status === "error" && (
        <div className="summary-workspace__grid">
          <article className="summary-workspace__card summary-workspace__card--error">
            <span className="section-label">Request Failed</span>
            <h3>summary 작업을 완료하지 못했습니다.</h3>
            <p>{errorMessage || "요약 작업 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."}</p>
            {sourceUrl && <p className="summary-workspace__meta-line">마지막 요청 URL: {sourceUrl}</p>}
            {taskId && <p className="summary-workspace__meta-line">Task ID: {taskId}</p>}
          </article>
        </div>
      )}

      {status === "success" && result && (
        <>
          <div className="summary-status summary-status--success">
            <span>{getCompletionMessage(completionState)}</span>
          </div>

          <div className="summary-workspace__grid summary-workspace__grid--result">
            <article className="summary-result-card summary-workspace__result-card">
              <div className="summary-result-card__header">
                <span className="section-label">Summary Result</span>
                {renderKnowledgePath(result.category, result.topic, result.keyword)}
              </div>

              <div className="summary-workspace__meta">
                <span>Summary ID: {result.summaryId}</span>
                <span>Task ID: {result.taskId}</span>
                <span>생성 시각: {formatDateTime(result.createdAt)}</span>
              </div>

              <div className="summary-result-card__body">
                <div className="summary-result-card__section">
                  <strong>요약 본문</strong>
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
            </article>

            <article className="summary-workspace__card">
              <span className="section-label">Next Action</span>
              <h3>Knowledge Graph에서 결과를 확인하세요.</h3>
              <p>
                summary 상세는 저장되었고, 경로 정보가 있으면 지식 트리에서도 같은 흐름을 확인할 수 있습니다.
              </p>
              <div className="summary-result-actions summary-workspace__actions">
                <button
                  className="button button--primary"
                  type="button"
                  onClick={() => navigate("/knowledge-graph")}
                >
                  Knowledge Graph 확인
                </button>
              </div>
            </article>
          </div>
        </>
      )}
    </section>
  );
}

function getProcessingStatusLabel(status: "idle" | "submitting" | "processing" | "success" | "error", streamStatus: "idle" | "connecting" | "live" | "error") {
  if (status === "submitting") {
    return "요약 작업 생성 중";
  }

  if (streamStatus === "connecting") {
    return "실시간 스트림 연결 중";
  }

  if (streamStatus === "live") {
    return "완료 이벤트 수신 대기 중";
  }

  return "처리 중";
}

function getCompletionMessage(completionState: "success" | "partial" | null) {
  if (completionState === "partial") {
    return "요약은 완료되었고, 가장 유사한 기존 키워드에 연결되었습니다.";
  }

  return "summary 결과가 저장되었고 상세 조회까지 완료되었습니다.";
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

function formatDateTime(value: string) {
  const parsedDate = new Date(value);

  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(parsedDate);
}
