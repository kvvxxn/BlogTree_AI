import { request } from "@/shared/api/http";
import type { UserProfile, UserProfileUpdateRequest } from "@/shared/types/api";

export function getMyProfile() {
  return request<UserProfile>("/api/users/me");
}

export function updateMyProfile(payload: UserProfileUpdateRequest) {
  return request<UserProfile>("/api/users/me", {
    method: "PATCH",
    body: payload,
  });
}
