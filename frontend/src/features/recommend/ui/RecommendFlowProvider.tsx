import type { PropsWithChildren } from "react";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { getRecommendation, requestRecommendation } from "@/features/recommend/api/recommend.api";
import type {
  RecommendationResponse,
  RecommendationTaskSuccessEvent,
  TaskExpiredEvent,
  TaskFailureEvent,
} from "@/shared/types/api";
import { ApiError } from "@/shared/api/http";
import { subscribeTask, type TaskSubscription } from "@/shared/api/sse";
import { logger } from "@/shared/lib/logger";

export type RecommendUiStatus = "idle" | "submitting" | "processing" | "success" | "error";
export type RecommendStreamStatus = "idle" | "connecting" | "live" | "error";

type RecommendFlowContextValue = {
  errorMessage: string;
  isBusy: boolean;
  requestRecommend: () => Promise<void>;
  resetRecommend: () => void;
  result: RecommendationResponse | null;
  status: RecommendUiStatus;
  streamStatus: RecommendStreamStatus;
  taskId: string | null;
};

type RecommendFlowState = {
  errorMessage: string;
  result: RecommendationResponse | null;
  status: RecommendUiStatus;
  streamStatus: RecommendStreamStatus;
  taskId: string | null;
};

const initialState: RecommendFlowState = {
  errorMessage: "",
  result: null,
  status: "idle",
  streamStatus: "idle",
  taskId: null,
};

const RecommendFlowContext = createContext<RecommendFlowContextValue | null>(null);

export function RecommendFlowProvider({ children }: PropsWithChildren) {
  const [state, setState] = useState<RecommendFlowState>(initialState);
  const subscriptionRef = useRef<TaskSubscription | null>(null);
  const runIdRef = useRef(0);

  const cleanupSubscription = useCallback(() => {
    subscriptionRef.current?.close();
    subscriptionRef.current = null;
  }, []);

  const resetRecommend = useCallback(() => {
    runIdRef.current += 1;
    cleanupSubscription();
    setState(initialState);
  }, [cleanupSubscription]);

  const resolveRecommendationResult = useCallback(async (runId: number, event: RecommendationTaskSuccessEvent) => {
    try {
      const result = await getRecommendation(event.recommendationId);

      if (runIdRef.current !== runId) {
        return;
      }

      setState({
        errorMessage: "",
        result,
        status: "success",
        streamStatus: "idle",
        taskId: result.taskId,
      });
    } catch (error) {
      if (runIdRef.current !== runId) {
        return;
      }

      logger.warn("recommend", "recommendation 상세 조회에 실패했습니다.", error);
      setState((previousState) => ({
        ...previousState,
        errorMessage: getErrorMessage(error, "추천 결과를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."),
        result: null,
        status: "error",
        streamStatus: "error",
      }));
    }
  }, []);

  const requestRecommend = useCallback(async () => {
    const runId = runIdRef.current + 1;
    runIdRef.current = runId;
    cleanupSubscription();

    setState({
      errorMessage: "",
      result: null,
      status: "submitting",
      streamStatus: "idle",
      taskId: null,
    });

    try {
      const response = await requestRecommendation();

      if (runIdRef.current !== runId) {
        return;
      }

      setState({
        errorMessage: "",
        result: null,
        status: "processing",
        streamStatus: "connecting",
        taskId: response.taskId,
      });

      let terminalEventReceived = false;
      const subscription = subscribeTask(
        response.taskId,
        (eventName, payload) => {
          if (runIdRef.current !== runId) {
            return;
          }

          if (eventName === "SUCCESS") {
            terminalEventReceived = true;
            cleanupSubscription();
            void resolveRecommendationResult(runId, payload as RecommendationTaskSuccessEvent);
            return;
          }

          if (eventName === "FAILED" || eventName === "EXPIRED") {
            terminalEventReceived = true;
            cleanupSubscription();
            const failureEvent = payload as TaskFailureEvent | TaskExpiredEvent;

            setState((previousState) => ({
              ...previousState,
              errorMessage: failureEvent.message || "추천 작업을 완료하지 못했습니다.",
              result: null,
              status: "error",
              streamStatus: "error",
            }));
          }
        },
        {
          onOpen() {
            if (runIdRef.current !== runId) {
              return;
            }

            setState((previousState) => ({
              ...previousState,
              streamStatus: "live",
            }));
          },
        },
      );

      subscriptionRef.current = subscription;

      void subscription.completed
        .then(() => {
          if (runIdRef.current !== runId || terminalEventReceived) {
            return;
          }

          setState((previousState) => ({
            ...previousState,
            errorMessage: "작업 이벤트 스트림이 예기치 않게 종료되었습니다. 다시 시도해주세요.",
            result: null,
            status: "error",
            streamStatus: "error",
          }));
        })
        .catch((error) => {
          if (runIdRef.current !== runId) {
            return;
          }

          logger.warn("recommend", "recommend SSE 구독에 실패했습니다.", error);
          setState((previousState) => ({
            ...previousState,
            errorMessage: getErrorMessage(
              error,
              "실시간 작업 상태를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.",
            ),
            result: null,
            status: "error",
            streamStatus: "error",
          }));
        });
    } catch (error) {
      if (runIdRef.current !== runId) {
        return;
      }

      logger.warn("recommend", "recommend 요청 생성에 실패했습니다.", error);
      setState({
        errorMessage: getErrorMessage(error, "추천 요청을 생성하지 못했습니다."),
        result: null,
        status: "error",
        streamStatus: "error",
        taskId: null,
      });
    }
  }, [cleanupSubscription, resolveRecommendationResult]);

  useEffect(() => {
    return () => {
      cleanupSubscription();
    };
  }, [cleanupSubscription]);

  const value = useMemo<RecommendFlowContextValue>(
    () => ({
      ...state,
      isBusy: state.status === "submitting" || state.status === "processing",
      requestRecommend,
      resetRecommend,
    }),
    [requestRecommend, resetRecommend, state],
  );

  return <RecommendFlowContext.Provider value={value}>{children}</RecommendFlowContext.Provider>;
}

export function useRecommendFlow() {
  const context = useContext(RecommendFlowContext);

  if (!context) {
    throw new Error("useRecommendFlow must be used within RecommendFlowProvider");
  }

  return context;
}

function getErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof ApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallbackMessage;
}
