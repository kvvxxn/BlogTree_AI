export type LoginRequest = {
  authorizationCode: string;
  redirectUri: string;
};

export type LoginResponse = {
  message: string;
  accessToken: string;
  refreshToken?: string;
};

export type UserProfile = {
  id: number;
  email: string;
  name: string;
  profileImageUrl: string | null;
  careerGoal: string | null;
};

export type UserProfileUpdateRequest = {
  name: string;
  careerGoal: string;
};

export type KnowledgeTree = Record<string, Record<string, string[]>>;

export type SummaryRequest = {
  sourceUrl: string;
};

export type SummaryResponse = {
  summaryId: number;
  taskId: string;
  category: string | null;
  topic: string | null;
  keyword: string | null;
  sourceUrl: string;
  content: string;
  createdAt: string;
};

export type RecommendationResponse = {
  recommendationId: number;
  taskId: string;
  userId: number;
  reason: string;
  category: string;
  topic: string;
  keyword: string;
  createdAt: string;
};

export type TaskResponse = {
  taskId: string;
};

export type SummaryTaskSuccessEvent = {
  taskId: string;
  summaryId: number;
  category: string;
  topic: string;
  keyword: string;
  summaryContent: string;
};

export type SummaryTaskPartialSuccessEvent = {
  taskId: string;
  summaryId: number;
  category: string;
  topic: string;
  keyword: string;
  summaryContent: string;
};

export type RecommendationTaskSuccessEvent = {
  taskId: string;
  recommendationId: number;
  reason: string;
  category: string;
  topic: string;
  keyword: string;
};

export type TaskFailureEvent = {
  taskId: string;
  code: string;
  message: string;
};

export type TaskExpiredEvent = {
  taskId: string;
  code: string;
  message: string;
};

export type ApiErrorResponse = {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
};
