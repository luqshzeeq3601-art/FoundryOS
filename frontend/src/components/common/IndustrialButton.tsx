import React, { ButtonHTMLAttributes } from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

interface IndustrialButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'hazard' | 'warning' | 'secondary' | 'outline';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
}

export const IndustrialButton: React.FC<IndustrialButtonProps> = ({
  children,
  className,
  variant = 'secondary',
  size = 'md',
  isLoading = false,
  disabled,
  ...props
}) => {
  const baseStyles = 'inline-flex items-center justify-center font-mono font-bold uppercase tracking-wider transition-colors duration-100 disabled:opacity-40 disabled:cursor-not-allowed select-none active:translate-y-[1px]';

  const sizeStyles = {
    sm: 'px-3 py-1.5 text-xs min-h-[36px]',
    md: 'px-4 py-2.5 text-sm min-h-[44px]',
    lg: 'px-6 py-3.5 text-base min-h-[48px]', // Touch target compliant
  };

  const variantStyles = {
    primary: 'bg-terminal-green text-black hover:bg-emerald-400 border border-terminal-green active:bg-emerald-500',
    hazard: 'bg-hazard-red text-white hover:bg-red-600 border border-hazard-red active:bg-red-700',
    warning: 'bg-hazard-amber text-black hover:bg-amber-400 border border-hazard-amber active:bg-amber-500',
    secondary: 'bg-industrial-800 text-industrial-100 hover:bg-industrial-700 border border-industrial-600 hover:border-industrial-500',
    outline: 'bg-transparent text-industrial-300 hover:text-white border border-substrate-border hover:border-industrial-400',
  };

  return (
    <button
      className={twMerge(clsx(baseStyles, sizeStyles[size], variantStyles[variant], className))}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <span className="flex items-center gap-2">
          <span className="inline-block w-3.5 h-3.5 border-2 border-current border-t-transparent animate-spin" />
          <span>PROCESSING...</span>
        </span>
      ) : (
        children
      )}
    </button>
  );
};
