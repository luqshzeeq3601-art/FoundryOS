import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { IndustrialButton } from './IndustrialButton';
import { getErrorMessage } from '../../utils/errors';

interface ErrorStateProps {
  title: string;
  error?: unknown;
  onRetry?: () => void;
  isRetrying?: boolean;
  compact?: boolean;
}

/** Shown when a query fails, so a failed request never looks like "no data". */
export const ErrorState: React.FC<ErrorStateProps> = ({
  title,
  error,
  onRetry,
  isRetrying = false,
  compact = false,
}) => (
  <div
    role="alert"
    className={`border border-hazard-red/60 bg-red-950/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
      compact ? 'p-3' : 'p-4'
    }`}
  >
    <div className="flex items-start gap-3">
      <AlertTriangle size={18} className="text-hazard-red shrink-0 mt-0.5" aria-hidden="true" />
      <div>
        <p className="text-sm font-bold text-hazard-red">{title}</p>
        {error !== undefined && (
          <p className="text-xs text-industrial-300 mt-0.5">{getErrorMessage(error)}</p>
        )}
      </div>
    </div>
    {onRetry && (
      <IndustrialButton variant="outline" size="sm" onClick={onRetry} isLoading={isRetrying} className="shrink-0">
        <RefreshCw size={14} className="mr-1.5" aria-hidden="true" />
        Retry
      </IndustrialButton>
    )}
  </div>
);
