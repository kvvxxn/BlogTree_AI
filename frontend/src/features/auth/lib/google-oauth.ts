import { env } from "@/shared/config/env";

const GOOGLE_SCOPE = "openid email profile";

export function buildGoogleAuthorizeUrl() {
  if (!env.googleClientId) {
    throw new Error("VITE_GOOGLE_CLIENT_ID가 설정되지 않았습니다.");
  }

  const searchParams = new URLSearchParams({
    client_id: env.googleClientId,
    redirect_uri: env.googleRedirectUri,
    response_type: "code",
    scope: GOOGLE_SCOPE,
    access_type: "offline",
    prompt: "consent",
  });

  return `${env.googleAuthBaseUrl}?${searchParams.toString()}`;
}
