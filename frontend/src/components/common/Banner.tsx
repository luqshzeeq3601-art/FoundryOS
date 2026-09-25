import React, { useEffect, useRef } from 'react';
import { X } from 'lucide-react';

interface BannerProps {
  tone: 'error' | 'success' | 'info';
  children: React.ReactNode;
  onDismiss: () => void;
  /** Auto-dismiss after this many ms. Errors stay until dismissed. */
  autoDismissMs?: number;
}

const toneStyles = {
  error: 'bg-red-950/80 border-hazard-red text-hazard-red',
  success: 'bg-emerald-950/80 border-terminal-green text-terminal-green',
  info: 'bg-industrial-850 border-industrial-600 text-industrial-100',
};

/** Page-level feedback message, announced to assistive technology. */
export const Banner: React.FC<BannerProps> = ({ tone, children, onDismiss, autoDismissMs }) => {
  const timeout = autoDismissMs ?? (tone === 'error' ? undefined : 6000);
  // Callers pass inline handlers; keep the latest one without restarting the timer on every render.
  const onDismissRef = useRef(onDismiss);
  onDismissRef.current = onDismiss;

  useEffect(() => {
    if (!timeout) return;
    const timer = window.setTimeout(() => onDismissRef.current(), timeout);
    return () => window.clearTimeout(timer);
  }, [timeout, children]);

  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={`p-3 border text-xs font-mono flex items-center justify-between gap-3 ${toneStyles[tone]}`}
    >
      <span>{children}</span>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss message"
        className="shrink-0 p-1 text-industrial-300 hover:text-white"
      >
        <X size={14} aria-hidden="true" />
      </button>
    </div>
  );
};
