import { Navigate, Outlet } from "react-router-dom";
import { isAuthenticated } from "@/shared/api/token-storage";

export function PublicOnlyRoute() {
  if (isAuthenticated()) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
