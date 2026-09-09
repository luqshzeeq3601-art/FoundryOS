import { useEffect, useRef, useState } from 'react';

interface UseHardwareBarcodeScannerOptions {
  onScan: (barcode: string) => void;
  enabled?: boolean;
  minChars?: number;
  maxIntervalMs?: number;
}

export const useHardwareBarcodeScanner = ({
  onScan,
  enabled = true,
  minChars = 3,
  maxIntervalMs = 60,
}: UseHardwareBarcodeScannerOptions) => {
  const [lastScanned, setLastScanned] = useState<string | null>(null);
  const [isReceivingWedge, setIsReceivingWedge] = useState<boolean>(false);

  const bufferRef = useRef<string>('');
  const lastKeyTimeRef = useRef<number>(0);
  const wedgeTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!enabled) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      const now = performance.now();
      const interval = now - lastKeyTimeRef.current;
      lastKeyTimeRef.current = now;

      // Enter or Tab indicates standard barcode terminator
      if (e.key === 'Enter' || e.key === 'Tab') {
        if (bufferRef.current.length >= minChars) {
          e.preventDefault();
          e.stopPropagation();
          const scannedCode = bufferRef.current.trim();
          bufferRef.current = '';
          setIsReceivingWedge(false);
          setLastScanned(scannedCode);
          onScan(scannedCode);
        } else {
          bufferRef.current = '';
          setIsReceivingWedge(false);
        }
        return;
      }

      // Ignore single modifier keys (Shift, Control, Alt, Meta)
      if (e.key.length > 1) {
        return;
      }

      // If interval between consecutive keys is too long, reset buffer (human typing)
      if (interval > maxIntervalMs && bufferRef.current.length > 0) {
        bufferRef.current = '';
        setIsReceivingWedge(false);
      }

      bufferRef.current += e.key;

      // If burst typing speed detected, indicate hardware wedge active
      if (bufferRef.current.length >= 3 && interval <= maxIntervalMs) {
        setIsReceivingWedge(true);

        if (wedgeTimeoutRef.current) clearTimeout(wedgeTimeoutRef.current);
        wedgeTimeoutRef.current = setTimeout(() => {
          setIsReceivingWedge(false);
        }, 500);
      }
    };

    window.addEventListener('keydown', handleKeyDown, true);
    return () => {
      window.removeEventListener('keydown', handleKeyDown, true);
      if (wedgeTimeoutRef.current) clearTimeout(wedgeTimeoutRef.current);
    };
  }, [enabled, minChars, maxIntervalMs, onScan]);

  return { lastScanned, isReceivingWedge };
};
