import React, { useEffect, useRef } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Header } from './components/common/Header';
import { Navigation, NAV_ITEMS, canAccessTab } from './components/common/Navigation';
import { useHashTab } from './hooks/useHashTab';
import { OfflineBanner } from './components/common/OfflineBanner';
import { LoginView } from './components/views/LoginView';
import { DashboardView } from './components/views/DashboardView';
import { MachinesView } from './components/views/MachinesView';
import { DowntimeView } from './components/views/DowntimeView';
import { ProductionView } from './components/views/ProductionView';
import { MaintenanceView } from './components/views/MaintenanceView';
import { AdminUsersView } from './components/views/AdminUsersView';
import { AuditLogView } from './components/views/AuditLogView';
import { LiveTelemetryView } from './components/views/LiveTelemetryView';
import { HierarchyManagementView } from './components/views/HierarchyManagementView';
import { EnterpriseFleetAnalyticsView } from './components/views/EnterpriseFleetAnalyticsView';
import { EdgeResilienceView } from './components/views/EdgeResilienceView';
import { DigitalSopView } from './components/views/DigitalSopView';
import { VibrationHealthView } from './components/views/VibrationHealthView';
import { ErpIntegrationHubView } from './components/views/ErpIntegrationHubView';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const MainLayout: React.FC = () => {
  const { isAuthenticated, isLoading, user, hasRole } = useAuth();
  const requestedTab = useHashTab('dashboard');
  const activeTab = canAccessTab(requestedTab, hasRole) ? requestedTab : 'dashboard';
  const mainRef = useRef<HTMLElement>(null);
  const isFirstRender = useRef(true);

  // Keep the document title in step with the page, and move focus to the new page for screen readers.
  useEffect(() => {
    if (!isAuthenticated) {
      document.title = 'Sign in · FoundryOS';
      return;
    }
    const label = NAV_ITEMS.find((item) => item.id === activeTab)?.label;
    document.title = label ? `${label} · FoundryOS` : 'FoundryOS';
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    mainRef.current?.focus();
  }, [activeTab, isAuthenticated]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-substrate-dark flex flex-col items-center justify-center p-4">
        <div role="status" className="flex items-center gap-3 text-white font-mono text-sm">
          <span className="inline-block w-4 h-4 border-2 border-hazard-red border-t-transparent animate-spin" aria-hidden="true" />
          <span>Loading FoundryOS…</span>
        </div>
      </div>
    );
  }

  // If not logged in or temporary password change required
  if (!isAuthenticated || user?.mustChangePassword) {
    return <LoginView />;
  }

  const renderActiveView = () => {
    switch (activeTab) {
      case 'fleet-analytics':
        return <EnterpriseFleetAnalyticsView />;
      case 'edge-resilience':
        return <EdgeResilienceView />;
      case 'digital-sop':
        return <DigitalSopView />;
      case 'vibration-health':
        return <VibrationHealthView />;
      case 'erp-sync':
        return <ErpIntegrationHubView />;
      case 'machines':
        return <MachinesView />;
      case 'telemetry':
        return <LiveTelemetryView />;
      case 'downtime':
        return <DowntimeView />;
      case 'production':
        return <ProductionView />;
      case 'maintenance':
        return <MaintenanceView />;
      case 'hierarchy':
        return <HierarchyManagementView />;
      case 'audit':
        return <AuditLogView />;
      case 'admin':
        return <AdminUsersView />;
      case 'dashboard':
      default:
        return <DashboardView />;
    }
  };

  return (
    <div className="min-h-screen bg-substrate-dark text-industrial-100 flex flex-col selection:bg-hazard-red selection:text-white">
      <a
        href="#main-content"
        onClick={(e) => {
          e.preventDefault();
          mainRef.current?.focus();
        }}
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-[60] focus:bg-white focus:text-black focus:px-3 focus:py-2 focus:text-sm"
      >
        Skip to content
      </a>
      <OfflineBanner />
      <Header />
      <Navigation activeTab={activeTab} />

      <main
        id="main-content"
        ref={mainRef}
        tabIndex={-1}
        className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 pb-12 focus:outline-none"
      >
        {renderActiveView()}
      </main>

      <footer className="border-t border-substrate-border bg-substrate-dark py-3 px-4 sm:px-6 text-xs font-mono text-industrial-400 flex flex-col sm:flex-row items-center justify-between gap-2">
        <div>FoundryOS {__APP_VERSION__}</div>
        <div>Changes are recorded in the audit log</div>
      </footer>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <MainLayout />
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
