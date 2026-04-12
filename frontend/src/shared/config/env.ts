const DEFAULT_API_BASE_URL = "http://localhost:8080";
const DEFAULT_GOOGLE_AUTH_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth";

function readEnvString(value: unknown, fallback: string) {
  return typeof value === "string" && value.length > 0 ? value : fallback;
}

export const env = {
  apiBaseUrl: readEnvString(import.meta.env.VITE_API_BASE_URL, DEFAULT_API_BASE_URL),
  googleClientId: readEnvString(import.meta.env.VITE_GOOGLE_CLIENT_ID, ""),
  googleRedirectUri: readEnvString(
    import.meta.env.VITE_GOOGLE_REDIRECT_URI,
    `${window.location.origin}/auth/callback`,
  ),
  googleAuthBaseUrl: readEnvString(
    import.meta.env.VITE_GOOGLE_AUTH_BASE_URL,
    DEFAULT_GOOGLE_AUTH_BASE_URL,
  ),
};
