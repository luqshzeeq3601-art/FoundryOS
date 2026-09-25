import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { IndustrialBadge } from './IndustrialBadge';
import { IndustrialButton } from './IndustrialButton';
import { PlantSwitcher } from './PlantSwitcher';
import { LogOut, Radio, User } from 'lucide-react';
import { useServerHealth, ServerHealth } from '../../hooks/useServerHealth';

const healthDisplay: Record<ServerHealth, { label: string; className: string }> = {
  up: { label: 'Online', className: 'text-terminal-green' },
  degraded: { label: 'Degraded', className: 'text-hazard-amber' },
  down: { label: 'Unreachable', className: 'text-hazard-red' },
  checking: { label: 'Checking…', className: 'text-industrial-300' },
};

export const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const [timeString, setTimeString] = useState('');
  const health = healthDisplay[useServerHealth()];

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeString(now.toISOString().replace('T', ' ').substring(0, 19) + ' UTC');
    };
    updateTime();
    const timer = setInterval(updateTime, 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <header className="border-b border-substrate-border bg-substrate-dark text-industrial-100 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 h-14 flex items-center justify-between gap-4">
        {/* Brand & Multi-Tenant Plant Switcher */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3">
            <div className="w-7 h-7 bg-hazard-red flex items-center justify-center font-black text-black text-sm select-none" aria-hidden="true">
              F
            </div>
            <div className="flex flex-col">
              <div className="flex items-center gap-2">
                <span className="font-extrabold font-mono tracking-wider text-base text-white">
                  FOUNDRY<span className="text-hazard-red">//</span>OS
                </span>
              </div>
            </div>
          </div>

          <div className="hidden sm:block border-l border-substrate-border pl-4">
            <PlantSwitcher />
          </div>
        </div>

        {/* Center Telemetry & Clock */}
        <div className="hidden md:flex items-center gap-6 text-xs font-mono text-industrial-400">
          <div role="status" className="flex items-center gap-2 border border-substrate-border px-2.5 py-1 bg-industrial-900">
            <Radio size={12} className={health.className} aria-hidden="true" />
            <span className="text-industrial-300">
              Server: <span className={health.className}>{health.label}</span>
            </span>
          </div>
          <time className="border border-substrate-border px-2.5 py-1 bg-industrial-900 text-industrial-200 tabular-nums">
            {timeString}
          </time>
        </div>

        {/* User Info & Controls */}
        <div className="flex items-center gap-3">
          {user && (
            <>
              <div className="hidden sm:flex flex-col items-end">
                <div className="flex items-center gap-1.5">
                  <User size={12} className="text-industrial-400" aria-hidden="true" />
                  <span className="text-xs font-bold text-white font-mono">{user.displayName}</span>
                </div>
                <div className="mt-0.5">
                  <IndustrialBadge 
                    size="sm"
                    variant={user.role === 'ADMIN' ? 'danger' : user.role === 'PRODUCTION_MANAGER' ? 'info' : 'default'}
                  >
                    {user.role}
                  </IndustrialBadge>
                </div>
              </div>

              <IndustrialButton
                variant="outline"
                size="sm"
                onClick={logout}
                aria-label="Sign out"
                className="px-2.5 py-1 text-xs min-w-[44px] min-h-[44px]"
              >
                <LogOut size={14} className="sm:mr-1" aria-hidden="true" />
                <span className="hidden sm:inline">Sign out</span>
              </IndustrialButton>
            </>
          )}
        </div>
      </div>
    </header>
  );
};
