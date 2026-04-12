import { request } from "@/shared/api/http";
import type { KnowledgeTree } from "@/shared/types/api";

export function getKnowledgeTree() {
  return request<KnowledgeTree>("/api/tree");
}
