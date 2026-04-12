import { request } from "@/shared/api/http";
import type { SummaryRequest, SummaryResponse, TaskResponse } from "@/shared/types/api";

export function requestSummary(payload: SummaryRequest) {
  return request<TaskResponse>("/api/summary", {
    method: "POST",
    body: payload,
  });
}

export function getSummary(summaryId: number) {
  return request<SummaryResponse>(`/api/summary/${summaryId}`);
}
