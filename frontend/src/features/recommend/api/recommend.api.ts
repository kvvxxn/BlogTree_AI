import { request } from "@/shared/api/http";
import type { RecommendationResponse, TaskResponse } from "@/shared/types/api";

export function requestRecommendation() {
  return request<TaskResponse>("/api/recommend", {
    method: "POST",
    body: {},
  });
}

export function getRecommendation(recommendationId: number) {
  return request<RecommendationResponse>(`/api/recommend/${recommendationId}`);
}
