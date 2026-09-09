import React from 'react';

interface MetricTileProps {
  label: string;
  tag?: string;
  value: string | number;
  unit?: string;
  subtext?: string;
  status?: 'normal' | 'warning' | 'danger' | 'success';
  className?: string;
}

export const MetricTile: React.FC<MetricTileProps> = ({
  label,
  tag,
  value,
  unit,
  subtext,
  status = 'normal',
  className = '',
}) => {
  const getStatusBorder = () => {
    switch (status) {
      case 'danger':
        return 'border-hazard-red/80 bg-red-950/20';
      case 'warning':
        return 'border-hazard-amber/80 bg-amber-950/20';
      case 'success':
        return 'border-terminal-green/80 bg-emerald-950/20';
      case 'normal':
      default:
        return 'border-substrate-border bg-substrate-card';
    }
  };

  const getValueColor = () => {
    switch (status) {
      case 'danger':
        return 'text-hazard-red';
      case 'warning':
        return 'text-hazard-amber';
      case 'success':
        return 'text-terminal-green';
      case 'normal':
      default:
        return 'text-white';
    }
  };

  return (
    <div className={`relative p-4 border ${getStatusBorder()} ${className} flex flex-col justify-between`}>
      {/* Tactical Corner Crosshairs */}
      <span className="absolute top-0 left-0 text-[10px] leading-none text-industrial-600 select-none">+</span>
      <span className="absolute top-0 right-0 text-[10px] leading-none text-industrial-600 select-none">+</span>
      <span className="absolute bottom-0 left-0 text-[10px] leading-none text-industrial-600 select-none">+</span>
      <span className="absolute bottom-0 right-0 text-[10px] leading-none text-industrial-600 select-none">+</span>

      {/* Header */}
      <div className="flex items-center justify-between gap-2 mb-2">
        <span className="text-[11px] font-mono uppercase tracking-widest text-industrial-400 truncate">
          {label}
        </span>
        {tag && (
          <span className="text-[10px] font-mono text-industrial-500 uppercase px-1 border border-industrial-700/60 bg-industrial-900 shrink-0">
            {tag}
          </span>
        )}
      </div>

      {/* Big Value Readout */}
      <div className="flex items-baseline gap-1.5 my-1">
        <span className={`text-2xl sm:text-3xl font-bold font-mono tracking-tight ${getValueColor()}`}>
          {value}
        </span>
        {unit && (
          <span className="text-xs font-mono text-industrial-400 uppercase">
            {unit}
          </span>
        )}
      </div>

      {/* Subtext */}
      {subtext && (
        <div className="mt-2 text-[11px] font-mono text-industrial-400 truncate border-t border-substrate-border pt-1.5">
          {subtext}
        </div>
      )}
    </div>
  );
};
