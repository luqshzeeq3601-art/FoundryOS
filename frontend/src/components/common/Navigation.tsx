import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { 
  LayoutDashboard, 
  Cpu, 
  AlertTriangle, 
  ClipboardList, 
  Wrench, 
  ShieldCheck, 
  Users,
  Radio,
  FolderTree,
  BarChart3,
  Server,
  Sliders,
  Activity
} from 'lucide-react';

export type TabId = 'dashboard' | 'fleet-analytics' | 'edge-resilience' | 'digital-sop' | 'vibration-health' | 'erp-sync' | 'machines' | 'telemetry' | 'downtime' | 'production' | 'maintenance' | 'hierarchy' | 'audit' | 'admin';

interface NavigationProps {
  activeTab: TabId;
  onTabChange: (tab: TabId) => void;
}

export const Navigation: React.FC<NavigationProps> = ({ activeTab, onTabChange }) => {
  const { hasRole } = useAuth();

  const navItems: Array<{ id: TabId; label: string; icon: React.ReactNode; roles?: string[] }> = [
    { id: 'dashboard', label: 'DASHBOARD', icon: <LayoutDashboard size={16} /> },
    { 
      id: 'fleet-analytics', 
      label: 'FLEET BENCHMARK', 
      icon: <BarChart3 size={16} />, 
      roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'VIEWER'] 
    },
    { 
      id: 'edge-resilience', 
      label: 'EDGE RESILIENCE', 
      icon: <Server size={16} />, 
      roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN'] 
    },
    { 
      id: 'digital-sop', 
      label: 'DIGITAL SOP // QUALITY', 
      icon: <Sliders size={16} />, 
      roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER'] 
    },
    { 
      id: 'vibration-health', 
      label: 'VIBRATION // FFT', 
      icon: <Activity size={16} />, 
      roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER'] 
    },
    { 
      id: 'erp-sync', 
      label: 'ERP // SCM SYNC', 
      icon: <FolderTree size={16} />, 
      roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER'] 
    },
    { id: 'machines', label: 'MACHINES', icon: <Cpu size={16} /> },
    { id: 'telemetry', label: 'TELEMETRY // IIoT', icon: <Radio size={16} /> },
    { id: 'downtime', label: 'DOWNTIME', icon: <AlertTriangle size={16} /> },
    { id: 'production', label: 'PRODUCTION', icon: <ClipboardList size={16} /> },
    { id: 'maintenance', label: 'MAINTENANCE', icon: <Wrench size={16} /> },
    { 
      id: 'hierarchy', 
      label: 'HIERARCHY', 
      icon: <FolderTree size={16} />, 
      roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER'] 
    },
    { 
      id: 'audit', 
      label: 'AUDIT LOG', 
      icon: <ShieldCheck size={16} />, 
      roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER'] 
    },
    { 
      id: 'admin', 
      label: 'USERS // IAM', 
      icon: <Users size={16} />, 
      roles: ['ADMIN'] 
    },
  ];

  const visibleItems = navItems.filter(item => {
    if (!item.roles) return true;
    return hasRole(...item.roles);
  });

  return (
    <nav className="border-b border-substrate-border bg-substrate-card/60 backdrop-blur">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <div className="flex space-x-1 sm:space-x-2 overflow-x-auto no-scrollbar py-2">
          {visibleItems.map(item => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                className={`flex items-center gap-2 px-3 sm:px-4 py-2 text-xs font-mono font-bold tracking-wider uppercase border transition-all whitespace-nowrap select-none touch-target ${
                  isActive
                    ? 'bg-industrial-800 text-white border-industrial-500 shadow-sm'
                    : 'bg-transparent text-industrial-400 border-transparent hover:text-industrial-200 hover:bg-industrial-850 hover:border-substrate-border'
                }`}
              >
                <span className={isActive ? 'text-hazard-red' : 'text-industrial-500'}>
                  {item.icon}
                </span>
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </nav>
  );
};
