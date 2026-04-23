const ACCESS_TOKEN_KEY = "blogtree.accessToken";
const REFRESH_TOKEN_KEY = "blogtree.refreshToken";

export function getAccessToken() {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

function decodeBase64Url(value: string) {
  const normalizedValue = value.replace(/-/g, "+").replace(/_/g, "/");
  const paddedValue = normalizedValue.padEnd(
    normalizedValue.length + ((4 - (normalizedValue.length % 4)) % 4),
    "=",
  );
  return window.atob(paddedValue);
}

function isExpiredAccessToken(accessToken: string) {
  try {
    const [, payload] = accessToken.split(".");
    if (!payload) {
      return true;
    }

    const parsedPayload = JSON.parse(decodeBase64Url(payload)) as { exp?: number };
    if (typeof parsedPayload.exp !== "number") {
      return true;
    }

    return parsedPayload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

export function setAccessToken(accessToken: string) {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function clearAuthTokens() {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function isAuthenticated() {
  const accessToken = getAccessToken();
  if (!accessToken) {
    return false;
  }

  if (isExpiredAccessToken(accessToken)) {
    clearAuthTokens();
    return false;
  }

  return true;
}
