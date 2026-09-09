import React, { useEffect, ReactNode } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  hazard?: boolean;
  children: ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  subtitle,
  hazard = false,
  children,
  maxWidth = 'lg',
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.style.overflow = 'unset';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const maxWidthClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/80 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Modal Dialog */}
      <div
        className={`relative w-full ${maxWidthClasses[maxWidth]} bg-substrate-card border-2 ${
          hazard ? 'border-hazard-red' : 'border-industrial-600'
        } shadow-2xl z-10 max-h-[90vh] flex flex-col`}
      >
        {/* Top Warning Accent Stripe */}
        {hazard && <div className="h-1.5 w-full bg-hazard-stripes shrink-0" />}

        {/* Header */}
        <div className="flex items-start justify-between p-4 sm:p-5 border-b border-substrate-border shrink-0 bg-industrial-900">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-mono text-industrial-500 uppercase tracking-widest">[ DIALOG // CMD ]</span>
            </div>
            <h2 className={`text-base sm:text-lg font-bold font-mono uppercase tracking-wide mt-0.5 ${
              hazard ? 'text-hazard-red' : 'text-white'
            }`}>
              {title}
            </h2>
            {subtitle && (
              <p className="text-xs font-mono text-industrial-400 mt-1">
                {subtitle}
              </p>
            )}
          </div>
          <button
            onClick={onClose}
            className="text-industrial-400 hover:text-white p-1 hover:bg-industrial-800 transition-colors border border-transparent hover:border-substrate-border"
            title="Close [ESC]"
          >
            <X size={18} />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-4 sm:p-6 overflow-y-auto font-mono text-sm space-y-4">
          {children}
        </div>
      </div>
    </div>
  );
};
