import React from 'react';
import { MachineStatus } from '../../types';

interface StatusBeaconProps {
  status: MachineStatus | string;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
}

export const StatusBeacon: React.FC<StatusBeaconProps> = ({
  status,
  size = 'md',
  showLabel = true,
}) => {
  const normalized = status.toUpperCase();

  const getStatusConfig = () => {
    switch (normalized) {
      case 'RUNNING':
        return {
          dotColor: 'bg-terminal-green shadow-[0_0_8px_#22C55E]',
          textColor: 'text-terminal-green',
          label: 'RUNNING',
          pulse: false,
        };
      case 'DOWN':
        return {
          dotColor: 'bg-hazard-red shadow-[0_0_12px_#FF2A2A] animate-beacon',
          textColor: 'text-hazard-red font-bold',
          label: 'DOWN',
          pulse: true,
        };
      case 'IDLE':
      default:
        return {
          dotColor: 'bg-hazard-amber shadow-[0_0_6px_#F59E0B]',
          textColor: 'text-hazard-amber',
          label: 'IDLE',
          pulse: false,
        };
    }
  };

  const config = getStatusConfig();

  const sizeClasses = {
    sm: 'w-2 h-2',
    md: 'w-2.5 h-2.5',
    lg: 'w-3.5 h-3.5',
  };

  return (
    <div className="inline-flex items-center gap-2">
      <span className={`inline-block shrink-0 ${sizeClasses[size]} ${config.dotColor}`} />
      {showLabel && (
        <span className={`text-xs font-mono uppercase tracking-wider ${config.textColor}`}>
          {config.label}
        </span>
      )}
    </div>
  );
};
