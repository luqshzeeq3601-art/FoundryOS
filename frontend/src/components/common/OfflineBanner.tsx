import React, { useState, useEffect } from 'react';
import { WifiOff } from 'lucide-react';

export const OfflineBanner: React.FC = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (isOnline) return null;

  return (
    <div className="bg-hazard-red text-white px-4 py-2 text-xs font-mono font-bold uppercase tracking-wider flex items-center justify-between border-b-2 border-black z-50 sticky top-0">
      <div className="flex items-center gap-2">
        <WifiOff size={16} className="animate-pulse" />
        <span>[ NETWORK OFFLINE ] — LOCAL CACHE MODE ACTIVE. RECONNECTING TELEMETRY STREAM...</span>
      </div>
      <span className="text-[10px] bg-black/40 px-2 py-0.5 border border-white/20">RETRYING</span>
    </div>
  );
};
