import { useNavigate } from "react-router-dom";
import { useRecommendFlow } from "@/features/recommend/ui/RecommendFlowProvider";

export function RecommendationWorkspace() {
  const navigate = useNavigate();
  const { errorMessage, result, status, streamStatus, taskId } = useRecommendFlow();

  return (
    <section className="page-stack summary-workspace">
      <article className="summary-workspace__hero">
        <span className="sidebar__eyebrow">Career Picks</span>
        <h2 className="summary-workspace__title">추천 요청부터 완료 결과 조회까지 한 화면에서 확인하세요.</h2>
        <p className="summary-workspace__copy">
          recommendation page는 추천 작업 생성, 인증 기반 SSE 구독, 최종 recommendation 상세 조회를 한 흐름으로 연결합니다.
        </p>
      </article>

      {status === "idle" && (
        <div className="summary-workspace__grid">
          <article className="summary-workspace__card">
            <span className="section-label">How It Works</span>
            <h3>현재 연결 흐름</h3>
            <p>
              추천 요청을 보내면 백엔드가 `taskId`를 반환하고, 프론트는 해당 task의 SSE 이벤트를 구독한 뒤
              완료 시 recommendation 상세를 조회합니다.
            </p>
          </article>

          <article className="summary-workspace__card">
            <span className="section-label">Ready State</span>
            <h3>추천 요청 대기 중</h3>
            <p>
              왼쪽 패널에서 추천 생성을 실행하면 이 영역에 진행 상태와 최종 추천 결과가 표시됩니다.
            </p>
          </article>
        </div>
      )}

      {(status === "submitting" || status === "processing") && (
        <div className="summary-workspace__grid">
          <article className="summary-workspace__card">
            <span className="section-label">Current Task</span>
            <h3>백엔드 추천 작업 진행 중</h3>
            <div className="summary-workspace__meta">
              <span>상태: {getProcessingStatusLabel(status, streamStatus)}</span>
              {taskId && <span>Task ID: {taskId}</span>}
            </div>
          </article>

          <article className="summary-workspace__card">
            <span className="section-label">Next Step</span>
            <h3>완료 이벤트를 기다리는 중</h3>
            <p>
              `success` 이벤트를 받으면 `GET /api/recommend/{'{recommendationId}'}`로 최종 결과를 조회합니다.
            </p>
          </article>
        </div>
      )}

      {status === "error" && (
        <div className="summary-workspace__grid">
          <article className="summary-workspace__card summary-workspace__card--error">
            <span className="section-label">Request Failed</span>
            <h3>recommend 작업을 완료하지 못했습니다.</h3>
            <p>{errorMessage || "추천 작업 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."}</p>
            {taskId && <p className="summary-workspace__meta-line">Task ID: {taskId}</p>}
          </article>
        </div>
      )}

      {status === "success" && result && (
        <>
          <div className="summary-status summary-status--success">
            <span>recommend 결과가 저장되었고 상세 조회까지 완료되었습니다.</span>
          </div>

          <div className="summary-workspace__grid summary-workspace__grid--result">
            <article className="summary-result-card summary-workspace__result-card">
              <div className="summary-result-card__header">
                <span className="section-label">Recommendation Result</span>
                {renderKnowledgePath(result.category, result.topic, result.keyword)}
              </div>

              <div className="summary-workspace__meta">
                <span>Recommendation ID: {result.recommendationId}</span>
                <span>Task ID: {result.taskId}</span>
                <span>생성 시각: {formatDateTime(result.createdAt)}</span>
              </div>

              <div className="summary-result-card__body">
                <div className="summary-result-card__section">
                  <strong>추천 이유</strong>
                  <p>{result.reason}</p>
                </div>
              </div>
            </article>

            <article className="summary-workspace__card">
              <span className="section-label">Next Action</span>
              <h3>Knowledge Graph에서 추천 결과를 이어서 확인하세요.</h3>
              <p>
                추천 결과는 저장되었고, 같은 카테고리와 주제를 기준으로 다음 학습 경로를 계속 탐색할 수 있습니다.
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

function getProcessingStatusLabel(
  status: "idle" | "submitting" | "processing" | "success" | "error",
  streamStatus: "idle" | "connecting" | "live" | "error",
) {
  if (status === "submitting") {
    return "추천 작업 생성 중";
  }

  if (streamStatus === "connecting") {
    return "실시간 스트림 연결 중";
  }

  if (streamStatus === "live") {
    return "완료 이벤트 수신 대기 중";
  }

  return "처리 중";
}

function renderKnowledgePath(category: string, topic: string, keyword: string) {
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
