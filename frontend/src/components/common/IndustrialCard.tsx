import React from 'react';

interface IndustrialCardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  className?: string;
  variant?: 'default' | 'accent' | 'hazard';
}

export const IndustrialCard: React.FC<IndustrialCardProps> = ({
  children,
  className = '',
  variant = 'default',
  ...props
}) => {
  const variantStyles = {
    default: 'bg-substrate-card/90 border-substrate-border',
    accent: 'bg-substrate-card/90 border-industrial-500',
    hazard: 'bg-substrate-card/90 border-hazard-red/50',
  };

  return (
    <div
      className={`border backdrop-blur shadow-sm transition-all ${variantStyles[variant]} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};
