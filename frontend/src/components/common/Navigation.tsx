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
  Activity,
  Network,
} from 'lucide-react';

export type TabId =
  | 'dashboard'
  | 'fleet-analytics'
  | 'edge-resilience'
  | 'digital-sop'
  | 'vibration-health'
  | 'erp-sync'
  | 'machines'
  | 'telemetry'
  | 'downtime'
  | 'production'
  | 'maintenance'
  | 'hierarchy'
  | 'audit'
  | 'admin';

type NavGroup = 'Operations' | 'Assets' | 'Analytics' | 'Admin';

interface NavItem {
  id: TabId;
  label: string;
  group: NavGroup;
  icon: React.ReactNode;
  roles?: string[];
}

const ALL_ROLES_EXCEPT_OPERATOR = ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER'];

export const NAV_ITEMS: NavItem[] = [
  { id: 'dashboard', label: 'Dashboard', group: 'Operations', icon: <LayoutDashboard size={16} /> },
  { id: 'production', label: 'Production', group: 'Operations', icon: <ClipboardList size={16} /> },
  { id: 'downtime', label: 'Downtime', group: 'Operations', icon: <AlertTriangle size={16} /> },
  { id: 'maintenance', label: 'Maintenance', group: 'Operations', icon: <Wrench size={16} /> },
  {
    id: 'digital-sop',
    label: 'SOP & quality',
    group: 'Operations',
    icon: <Sliders size={16} />,
    roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER'],
  },
  { id: 'machines', label: 'Machines', group: 'Assets', icon: <Cpu size={16} /> },
  { id: 'telemetry', label: 'Telemetry', group: 'Assets', icon: <Radio size={16} /> },
  { id: 'vibration-health', label: 'Vibration', group: 'Assets', icon: <Activity size={16} />, roles: ALL_ROLES_EXCEPT_OPERATOR },
  {
    id: 'edge-resilience',
    label: 'Edge gateways',
    group: 'Assets',
    icon: <Server size={16} />,
    roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN'],
  },
  {
    id: 'fleet-analytics',
    label: 'Fleet benchmark',
    group: 'Analytics',
    icon: <BarChart3 size={16} />,
    roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'VIEWER'],
  },
  { id: 'erp-sync', label: 'ERP sync', group: 'Analytics', icon: <Network size={16} />, roles: ALL_ROLES_EXCEPT_OPERATOR },
  { id: 'hierarchy', label: 'Hierarchy', group: 'Admin', icon: <FolderTree size={16} />, roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER'] },
  { id: 'audit', label: 'Audit log', group: 'Admin', icon: <ShieldCheck size={16} />, roles: ['ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER'] },
  { id: 'admin', label: 'Users', group: 'Admin', icon: <Users size={16} />, roles: ['ADMIN'] },
];

export const isTabId = (value: string): value is TabId => NAV_ITEMS.some((item) => item.id === value);

export const canAccessTab = (tab: TabId, hasRole: (...roles: string[]) => boolean): boolean => {
  const item = NAV_ITEMS.find((i) => i.id === tab);
  return !!item && (!item.roles || hasRole(...item.roles));
};

interface NavigationProps {
  activeTab: TabId;
}

export const Navigation: React.FC<NavigationProps> = ({ activeTab }) => {
  const { hasRole } = useAuth();
  const visibleItems = NAV_ITEMS.filter((item) => canAccessTab(item.id, hasRole));

  return (
    <nav aria-label="Main" className="border-b border-substrate-border bg-substrate-card">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <ul className="flex items-stretch overflow-x-auto no-scrollbar py-2">
          {visibleItems.map((item, index) => {
            const isActive = activeTab === item.id;
            const startsGroup = index > 0 && visibleItems[index - 1].group !== item.group;
            return (
              <li
                key={item.id}
                className={`flex shrink-0 ${startsGroup ? 'ml-2 pl-2 border-l border-substrate-border' : 'ml-1 first:ml-0'}`}
              >
                <a
                  href={`#/${item.id}`}
                  aria-current={isActive ? 'page' : undefined}
                  className={`flex items-center gap-2 px-3 text-xs font-mono font-bold tracking-wider uppercase border transition-colors whitespace-nowrap select-none touch-target ${
                    isActive
                      ? 'bg-industrial-800 text-white border-industrial-500'
                      : 'bg-transparent text-industrial-300 border-transparent hover:text-white hover:bg-industrial-850 hover:border-substrate-border'
                  }`}
                >
                  <span className={isActive ? 'text-hazard-red' : 'text-industrial-500'} aria-hidden="true">
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </a>
              </li>
            );
          })}
        </ul>
      </div>
    </nav>
  );
};
