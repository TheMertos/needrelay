import { MantineProvider } from '@mantine/core';
import '@mantine/core/styles.css';
import { Notifications } from '@mantine/notifications';
import '@mantine/notifications/styles.css';
import { useEffect, type ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { getAccessToken } from './api/client';
import { setSessionExpiredHandler } from './api/session';
import { AppLayout } from './components/AppLayout';
import { AccountPage } from './pages/AccountPage';
import { AdminOrganizersPage } from './pages/AdminOrganizersPage';
import { DashboardPage } from './pages/DashboardPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { InvitesPage } from './pages/InvitesPage';
import { DiscoveryPage } from './pages/DiscoveryPage';
import { LoginPage } from './pages/LoginPage';
import { ManageRequestPage } from './pages/ManageRequestPage';
import { NewRequestPage } from './pages/NewRequestPage';
import { PublicReliefPage } from './pages/PublicReliefPage';
import { RegisterPage } from './pages/RegisterPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { theme } from './theme';
import './i18n';

/**
 * Protects organizer routes by requiring an access token.
 *
 * @param props children to render when authenticated
 * @returns children or redirect
 */
function RequireAuth({ children }: { children: ReactNode }) {
  if (!getAccessToken()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

/**
 * Wires Axios session-expired redirects to React Router without circular imports.
 *
 * @returns null
 */
function SessionBridge() {
  const navigate = useNavigate();

  useEffect(() => {
    setSessionExpiredHandler(() => {
      navigate('/login', { replace: true });
    });
    return () => setSessionExpiredHandler(null);
  }, [navigate]);

  return null;
}

/**
 * Root application with Mantine provider and routes.
 *
 * @returns app tree
 */
export default function App() {
  return (
    <MantineProvider theme={theme} defaultColorScheme="light">
      <Notifications position="top-right" />
      <BrowserRouter>
        <SessionBridge />
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<DiscoveryPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="forgot-password" element={<ForgotPasswordPage />} />
            <Route path="reset-password" element={<ResetPasswordPage />} />
            <Route path="r/:slug" element={<PublicReliefPage />} />
            <Route
              path="dashboard"
              element={
                <RequireAuth>
                  <DashboardPage />
                </RequireAuth>
              }
            />
            <Route
              path="account"
              element={
                <RequireAuth>
                  <AccountPage />
                </RequireAuth>
              }
            />
            <Route
              path="admin/organizers"
              element={
                <RequireAuth>
                  <AdminOrganizersPage />
                </RequireAuth>
              }
            />
            <Route
              path="invites"
              element={
                <RequireAuth>
                  <InvitesPage />
                </RequireAuth>
              }
            />
            <Route
              path="requests/new"
              element={
                <RequireAuth>
                  <NewRequestPage />
                </RequireAuth>
              }
            />
            <Route
              path="requests/:requestId"
              element={
                <RequireAuth>
                  <ManageRequestPage />
                </RequireAuth>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </MantineProvider>
  );
}
