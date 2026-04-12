const REDIRECT_AFTER_LOGIN_KEY = "blogtree.redirectAfterLogin";

export function saveRedirectAfterLogin(path: string) {
  window.sessionStorage.setItem(REDIRECT_AFTER_LOGIN_KEY, path);
}

export function consumeRedirectAfterLogin() {
  const path = window.sessionStorage.getItem(REDIRECT_AFTER_LOGIN_KEY);
  window.sessionStorage.removeItem(REDIRECT_AFTER_LOGIN_KEY);
  return path ?? "/";
}
