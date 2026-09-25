import React, { useEffect, useId, useRef, ReactNode } from 'react';
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

const FOCUSABLE =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

const maxWidthClasses = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl',
  '2xl': 'max-w-2xl',
};

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  subtitle,
  hazard = false,
  children,
  maxWidth = 'lg',
}) => {
  const titleId = useId();
  const subtitleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!isOpen) return;

    const previouslyFocused = document.activeElement as HTMLElement | null;
    const dialog = dialogRef.current;

    // Focus the first form field, falling back to the first focusable control.
    const firstField = dialog?.querySelector<HTMLElement>('input, select, textarea');
    const firstFocusable = dialog?.querySelector<HTMLElement>(FOCUSABLE);
    (firstField ?? firstFocusable ?? dialog)?.focus();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onCloseRef.current();
        return;
      }
      if (e.key !== 'Tab' || !dialog) return;

      const focusable = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (focusable.length === 0) {
        e.preventDefault();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
      previouslyFocused?.focus?.();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-black/80" aria-hidden="true" onClick={onClose} />

      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={subtitle ? subtitleId : undefined}
        tabIndex={-1}
        className={`relative w-full ${maxWidthClasses[maxWidth]} bg-substrate-card border-2 ${
          hazard ? 'border-hazard-red' : 'border-industrial-600'
        } z-10 max-h-[90vh] flex flex-col focus:outline-none`}
      >
        {hazard && <div className="h-1.5 w-full bg-hazard-stripes shrink-0" aria-hidden="true" />}

        <div className="flex items-start justify-between gap-4 p-4 sm:p-5 border-b border-substrate-border shrink-0 bg-industrial-900">
          <div>
            <h2
              id={titleId}
              className={`text-base sm:text-lg font-bold font-mono uppercase tracking-wide ${
                hazard ? 'text-hazard-red' : 'text-white'
              }`}
            >
              {title}
            </h2>
            {subtitle && (
              <p id={subtitleId} className="text-xs font-mono text-industrial-400 mt-1">
                {subtitle}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close dialog"
            className="touch-target -m-2 flex items-center justify-center text-industrial-400 hover:text-white hover:bg-industrial-800 transition-colors"
          >
            <X size={18} aria-hidden="true" />
          </button>
        </div>

        <div className="p-4 sm:p-6 overflow-y-auto font-mono text-sm space-y-4">{children}</div>
      </div>
    </div>
  );
};
