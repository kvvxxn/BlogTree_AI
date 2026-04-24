import { request } from "@/shared/api/http";
import type { StatsResponse } from "@/shared/types/api";

export function getStats() {
  return request<StatsResponse>("/api/stats");
}
