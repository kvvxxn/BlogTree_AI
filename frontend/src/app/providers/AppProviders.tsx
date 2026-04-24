import type { PropsWithChildren } from "react";
import { BrowserRouter } from "react-router-dom";
import { AuthSessionProvider } from "@/features/auth/providers/AuthSessionProvider";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <BrowserRouter
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <AuthSessionProvider>{children}</AuthSessionProvider>
    </BrowserRouter>
  );
}
