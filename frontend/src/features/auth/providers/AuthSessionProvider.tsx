import type { PropsWithChildren } from "react";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { reissueAccessToken } from "@/shared/api/http";
import { clearAuthTokens, isAuthenticated, setAccessToken } from "@/shared/api/token-storage";
import { logger } from "@/shared/lib/logger";

type AuthStatus = "checking" | "authenticated" | "unauthenticated";

type AuthSessionContextValue = {
  status: AuthStatus;
  markAuthenticated: (accessToken: string) => void;
  markLoggedOut: () => void;
};

const AuthSessionContext = createContext<AuthSessionContextValue | null>(null);

export function AuthSessionProvider({ children }: PropsWithChildren) {
  const [status, setStatus] = useState<AuthStatus>(() =>
    isAuthenticated() ? "authenticated" : "checking",
  );

  useEffect(() => {
    if (isAuthenticated()) {
      return;
    }

    let cancelled = false;

    async function restoreSession() {
      try {
        logger.info("auth", "저장된 access token이 없어 cookie 기반 세션 복구를 시도합니다.");
        const restored = await reissueAccessToken();

        if (cancelled) {
          return;
        }

        setStatus(restored ? "authenticated" : "unauthenticated");
      } catch (error) {
        if (cancelled) {
          return;
        }

        logger.warn("auth", "초기 세션 복구에 실패했습니다.", error);
        clearAuthTokens();
        setStatus("unauthenticated");
      }
    }

    void restoreSession();

    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthSessionContextValue>(
    () => ({
      status,
      markAuthenticated(accessToken: string) {
        setAccessToken(accessToken);
        setStatus("authenticated");
      },
      markLoggedOut() {
        clearAuthTokens();
        setStatus("unauthenticated");
      },
    }),
    [status],
  );

  return <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>;
}

export function useAuthSession() {
  const context = useContext(AuthSessionContext);

  if (!context) {
    throw new Error("useAuthSession must be used within AuthSessionProvider");
  }

  return context;
}
