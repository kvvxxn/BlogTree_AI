import { Navigate, Outlet, useLocation } from "react-router-dom";
import { isAuthenticated } from "@/shared/api/token-storage";
import { saveRedirectAfterLogin } from "@/features/auth/lib/auth-redirect";

export function ProtectedRoute() {
  const location = useLocation();

  if (!isAuthenticated()) {
    saveRedirectAfterLogin(`${location.pathname}${location.search}${location.hash}`);
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
