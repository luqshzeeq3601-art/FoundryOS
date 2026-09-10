import React, { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Header } from './components/common/Header';
import { Navigation, TabId } from './components/common/Navigation';
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

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const MainLayout: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();
  const [activeTab, setActiveTab] = useState<TabId>('dashboard');

  if (isLoading) {
    return (
      <div className="min-h-screen bg-substrate-dark flex flex-col items-center justify-center p-4">
        <div className="flex items-center gap-3 text-white font-mono text-sm">
          <span className="inline-block w-4 h-4 border-2 border-hazard-red border-t-transparent animate-spin" />
          <span>INITIALIZING FOUNDRY//OS TELEMETRY BUS...</span>
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
      <OfflineBanner />
      <Header />
      <Navigation activeTab={activeTab} onTabChange={setActiveTab} />
      
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 pb-12">
        {renderActiveView()}
      </main>

      <footer className="border-t border-substrate-border bg-substrate-dark py-3 px-4 sm:px-6 text-[10px] font-mono text-industrial-500 flex flex-col sm:flex-row items-center justify-between gap-2">
        <div>FOUNDRY//OS v1.0.0-PROD // DEPLOYMENT: SINGLE-PLANT OPERATING SYSTEM</div>
        <div>ALL TRANSACTIONS LOGGED UNDER SHA-256 SECURED AUDIT BUS</div>
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
