import React from 'react';

interface IndustrialBadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'danger' | 'warning' | 'success' | 'info' | 'critical';
  size?: 'sm' | 'md';
}

export const IndustrialBadge: React.FC<IndustrialBadgeProps> = ({
  children,
  variant = 'default',
  size = 'md',
}) => {
  const getStyles = () => {
    switch (variant) {
      case 'danger':
      case 'critical':
        return 'bg-red-950/80 text-hazard-red border-hazard-red/60';
      case 'warning':
        return 'bg-amber-950/80 text-hazard-amber border-hazard-amber/60';
      case 'success':
        return 'bg-emerald-950/80 text-terminal-green border-terminal-green/60';
      case 'info':
        return 'bg-cyan-950/80 text-terminal-cyan border-terminal-cyan/60';
      case 'default':
      default:
        return 'bg-industrial-850 text-industrial-300 border-substrate-border';
    }
  };

  const sizeClass = size === 'sm' ? 'px-1.5 py-0.5 text-[10px]' : 'px-2 py-1 text-xs';

  return (
    <span
      className={`inline-flex items-center font-mono font-medium uppercase tracking-wider border select-none ${sizeClass} ${getStyles()}`}
    >
      {children}
    </span>
  );
};
