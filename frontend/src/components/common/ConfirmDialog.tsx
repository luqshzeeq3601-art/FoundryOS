import React from 'react';
import { Modal } from './Modal';
import { IndustrialButton } from './IndustrialButton';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: React.ReactNode;
  confirmLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
  hazard?: boolean;
}

/** Replaces window.confirm() so destructive actions stay inside the design system and remain keyboard accessible. */
export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  confirmLabel,
  onConfirm,
  onCancel,
  isLoading = false,
  hazard = true,
}) => (
  <Modal isOpen={isOpen} onClose={onCancel} title={title} hazard={hazard} maxWidth="md">
    <p className="text-sm text-industrial-200 leading-relaxed">{message}</p>
    <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
      <IndustrialButton type="button" variant="outline" onClick={onCancel}>
        Cancel
      </IndustrialButton>
      <IndustrialButton
        type="button"
        variant={hazard ? 'hazard' : 'secondary'}
        isLoading={isLoading}
        onClick={onConfirm}
      >
        {confirmLabel}
      </IndustrialButton>
    </div>
  </Modal>
);
