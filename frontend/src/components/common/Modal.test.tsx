import React, { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

const Harness: React.FC<{ onClose?: () => void }> = ({ onClose }) => {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button onClick={() => setOpen(true)}>Open</button>
      <Modal
        isOpen={open}
        title="Edit machine"
        subtitle="Change details"
        onClose={() => {
          onClose?.();
          setOpen(false);
        }}
      >
        <label htmlFor="name">Name</label>
        <input id="name" />
        <button type="button">Save</button>
      </Modal>
    </>
  );
};

describe('Modal', () => {
  it('exposes dialog semantics tied to its title and subtitle', async () => {
    render(<Harness />);
    await userEvent.click(screen.getByRole('button', { name: 'Open' }));

    const dialog = screen.getByRole('dialog', { name: 'Edit machine' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAccessibleDescription('Change details');
  });

  it('focuses the first field, traps Tab inside, and restores focus on Escape', async () => {
    const onClose = vi.fn();
    render(<Harness onClose={onClose} />);
    const trigger = screen.getByRole('button', { name: 'Open' });
    await userEvent.click(trigger);

    const input = screen.getByLabelText('Name');
    expect(input).toHaveFocus();

    await userEvent.tab(); // Save
    await userEvent.tab(); // wraps to close button (first focusable)
    expect(screen.getByRole('button', { name: 'Close dialog' })).toHaveFocus();

    await userEvent.tab({ shift: true }); // wraps back to Save (last focusable)
    expect(screen.getByRole('button', { name: 'Save' })).toHaveFocus();

    await userEvent.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });
});
