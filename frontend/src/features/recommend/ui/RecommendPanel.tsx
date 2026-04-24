import { useNavigate } from "react-router-dom";
import { useRecommendFlow } from "@/features/recommend/ui/RecommendFlowProvider";

export function RecommendPanel() {
  const navigate = useNavigate();
  const { errorMessage, isBusy, requestRecommend, resetRecommend, result, status, streamStatus, taskId } =
    useRecommendFlow();

  const isSuccess = status === "success" && result;

  function handleReset() {
    resetRecommend();
  }

  function handleGoToKnowledgeGraph() {
    navigate("/knowledge-graph");
  }

  const progressMessage = getProgressMessage(streamStatus);

  return (
    <section className="sidebar__panel">
      <div>
        <span className="sidebar__eyebrow">Learning Recommendation</span>
        <h2 className="sidebar__panel-title">맞춤형 학습 추천</h2>
      </div>
      <p className="sidebar__panel-copy">
        현재까지 구축한 블로그 트리와 설정하신 커리어 목표를 분석하여, 다음으로 학습하기 좋은 맞춤형 키워드를 추천합니다.
      </p>

      {!isSuccess && (
        <button
          className="button button--primary summary-button"
          type="button"
          onClick={() => void requestRecommend()}
          disabled={isBusy}
        >
          {isBusy ? (
            <>
              <span className="summary-button__spinner" />
              {getSubmitButtonLabel(status, streamStatus)}
            </>
          ) : (
            "추천 키워드 생성"
          )}
        </button>
      )}

      {status === "processing" && (
        <div className="summary-status summary-status--success">
          <span>{progressMessage}</span>
          {taskId && <span className="summary-status__meta">Task ID: {taskId}</span>}
        </div>
      )}

      {status === "error" && errorMessage && (
        <div className="summary-status summary-status--error">
          <span>{errorMessage}</span>
        </div>
      )}

      {isSuccess && result && (
        <>
          <div className="summary-status summary-status--success">
            <span>추천 결과가 저장되었고 상세 조회까지 완료되었습니다.</span>
          </div>

          <hr className="summary-divider" />
          <div className="summary-result-card">
            <div className="summary-result-card__header">
              <span className="section-label">Recommend Card</span>
              {renderKnowledgePath(result.category, result.topic, result.keyword)}
            </div>

            <div className="summary-result-card__body">
              <div className="summary-result-card__section">
                <strong>추천 이유</strong>
                <p>{result.reason}</p>
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

function getSubmitButtonLabel(
  status: "idle" | "submitting" | "processing" | "success" | "error",
  streamStatus: "idle" | "connecting" | "live" | "error",
) {
  if (status === "submitting") {
    return "추천 작업 생성 중...";
  }

  if (status === "processing") {
    if (streamStatus === "connecting") {
      return "실시간 연결 중...";
    }

    return "추천 결과 수신 대기 중...";
  }

  return "추천 키워드 생성";
}

function getProgressMessage(streamStatus: "idle" | "connecting" | "live" | "error") {
  if (streamStatus === "connecting") {
    return "작업은 생성되었습니다. 실시간 결과 스트림에 연결하는 중입니다.";
  }

  if (streamStatus === "live") {
    return "실시간 연결이 열렸습니다. 백엔드 작업 완료 이벤트를 기다리는 중입니다.";
  }

  return "작업 진행 상태를 확인하는 중입니다.";
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
