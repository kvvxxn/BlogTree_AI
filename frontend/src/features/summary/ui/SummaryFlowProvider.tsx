import type { PropsWithChildren } from "react";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { getSummary, requestSummary } from "@/features/summary/api/summary.api";
import type {
  SummaryResponse,
  SummaryTaskPartialSuccessEvent,
  SummaryTaskSuccessEvent,
  TaskExpiredEvent,
  TaskFailureEvent,
} from "@/shared/types/api";
import { ApiError } from "@/shared/api/http";
import { subscribeTask, type TaskSubscription } from "@/shared/api/sse";
import { logger } from "@/shared/lib/logger";

export type SummaryUiStatus = "idle" | "submitting" | "processing" | "success" | "error";
export type SummaryStreamStatus = "idle" | "connecting" | "live" | "error";
export type SummaryCompletionState = "success" | "partial" | null;

type SummaryFlowContextValue = {
  completionState: SummaryCompletionState;
  errorMessage: string;
  isBusy: boolean;
  resetSummary: () => void;
  result: SummaryResponse | null;
  sourceUrl: string;
  status: SummaryUiStatus;
  streamStatus: SummaryStreamStatus;
  submitSummary: (sourceUrl: string) => Promise<void>;
  taskId: string | null;
};

type SummaryFlowState = {
  completionState: SummaryCompletionState;
  errorMessage: string;
  result: SummaryResponse | null;
  sourceUrl: string;
  status: SummaryUiStatus;
  streamStatus: SummaryStreamStatus;
  taskId: string | null;
};

const initialState: SummaryFlowState = {
  completionState: null,
  errorMessage: "",
  result: null,
  sourceUrl: "",
  status: "idle",
  streamStatus: "idle",
  taskId: null,
};

const SummaryFlowContext = createContext<SummaryFlowContextValue | null>(null);

export function SummaryFlowProvider({ children }: PropsWithChildren) {
  const [state, setState] = useState<SummaryFlowState>(initialState);
  const subscriptionRef = useRef<TaskSubscription | null>(null);
  const runIdRef = useRef(0);

  const cleanupSubscription = useCallback(() => {
    subscriptionRef.current?.close();
    subscriptionRef.current = null;
  }, []);

  const resetSummary = useCallback(() => {
    runIdRef.current += 1;
    cleanupSubscription();
    setState(initialState);
  }, [cleanupSubscription]);

  const resolveSummaryResult = useCallback(
    async (
      runId: number,
      event: SummaryTaskSuccessEvent | SummaryTaskPartialSuccessEvent,
      completionState: SummaryCompletionState,
    ) => {
      try {
        const result = await getSummary(event.summaryId);

        if (runIdRef.current !== runId) {
          return;
        }

        setState({
          completionState,
          errorMessage: "",
          result,
          sourceUrl: result.sourceUrl,
          status: "success",
          streamStatus: "idle",
          taskId: result.taskId,
        });
      } catch (error) {
        if (runIdRef.current !== runId) {
          return;
        }

        logger.warn("summary", "summary 상세 조회에 실패했습니다.", error);
        setState((previousState) => ({
          ...previousState,
          completionState: null,
          errorMessage: getErrorMessage(error, "요약 결과를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."),
          result: null,
          status: "error",
          streamStatus: "error",
        }));
      }
    },
    [],
  );

  const submitSummary = useCallback(
    async (sourceUrl: string) => {
      const runId = runIdRef.current + 1;
      runIdRef.current = runId;
      cleanupSubscription();

      setState({
        completionState: null,
        errorMessage: "",
        result: null,
        sourceUrl,
        status: "submitting",
        streamStatus: "idle",
        taskId: null,
      });

      try {
        const response = await requestSummary({ sourceUrl });

        if (runIdRef.current !== runId) {
          return;
        }

        setState({
          completionState: null,
          errorMessage: "",
          result: null,
          sourceUrl,
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

            if (eventName === "SUCCESS" || eventName === "PARTIAL_SUCCESS") {
              terminalEventReceived = true;
              cleanupSubscription();
              void resolveSummaryResult(
                runId,
                payload as SummaryTaskSuccessEvent | SummaryTaskPartialSuccessEvent,
                eventName === "PARTIAL_SUCCESS" ? "partial" : "success",
              );
              return;
            }

            if (eventName === "FAILED" || eventName === "EXPIRED") {
              terminalEventReceived = true;
              cleanupSubscription();
              const failureEvent = payload as TaskFailureEvent | TaskExpiredEvent;

              setState((previousState) => ({
                ...previousState,
                completionState: null,
                errorMessage: failureEvent.message || "요약 작업을 완료하지 못했습니다.",
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
              completionState: null,
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

            logger.warn("summary", "summary SSE 구독에 실패했습니다.", error);
            setState((previousState) => ({
              ...previousState,
              completionState: null,
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

        logger.warn("summary", "summary 요청 생성에 실패했습니다.", error);
        setState({
          completionState: null,
          errorMessage: getErrorMessage(error, "요약 요청을 생성하지 못했습니다."),
          result: null,
          sourceUrl,
          status: "error",
          streamStatus: "error",
          taskId: null,
        });
      }
    },
    [cleanupSubscription, resolveSummaryResult],
  );

  useEffect(() => {
    return () => {
      cleanupSubscription();
    };
  }, [cleanupSubscription]);

  const value = useMemo<SummaryFlowContextValue>(
    () => ({
      ...state,
      isBusy: state.status === "submitting" || state.status === "processing",
      resetSummary,
      submitSummary,
    }),
    [resetSummary, state, submitSummary],
  );

  return <SummaryFlowContext.Provider value={value}>{children}</SummaryFlowContext.Provider>;
}

export function useSummaryFlow() {
  const context = useContext(SummaryFlowContext);

  if (!context) {
    throw new Error("useSummaryFlow must be used within SummaryFlowProvider");
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
