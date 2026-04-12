import type {
  RecommendationTaskSuccessEvent,
  SummaryTaskSuccessEvent,
  TaskFailureEvent,
} from "@/shared/types/api";

export type TaskTerminalEventName = "success" | "partial_success" | "failed" | "expired";

export type TaskEventPayload =
  | SummaryTaskSuccessEvent
  | RecommendationTaskSuccessEvent
  | TaskFailureEvent;
