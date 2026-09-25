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
    <div role="alert" className="bg-hazard-red text-white px-4 py-2 text-xs font-mono font-bold flex items-center justify-between border-b-2 border-black z-50 sticky top-0">
      <div className="flex items-center gap-2">
        <WifiOff size={16} aria-hidden="true" />
        <span>This device is offline. Data shown may be out of date until the connection returns.</span>
      </div>
    </div>
  );
};
