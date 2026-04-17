import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "@/pages/LoginPage";
import { AuthCallbackPage } from "@/pages/AuthCallbackPage";
import { HomePage } from "@/pages/HomePage";
import { DashboardPage } from "@/pages/DashboardPage";
import { SummaryPage } from "@/pages/SummaryPage";
import { StatsPage } from "@/pages/StatsPage";
import { RecommendationPage } from "@/pages/RecommendationPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { AppLayout } from "@/widgets/layout/AppLayout";
import { ProtectedRoute } from "@/features/auth/ui/ProtectedRoute";
import { PublicOnlyRoute } from "@/features/auth/ui/PublicOnlyRoute";

export function AppRouter() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/knowledge-graph" element={<DashboardPage />} />
          <Route path="/summary" element={<SummaryPage />} />
          <Route path="/stats" element={<StatsPage />} />
          <Route path="/recommendation" element={<RecommendationPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
