import { describe, expect, it, vi, afterEach } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { Banner } from './Banner';

describe('Banner', () => {
  afterEach(() => vi.useRealTimers());

  it('announces errors as alerts and keeps them until dismissed', () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    render(<Banner tone="error" onDismiss={onDismiss}>Save failed</Banner>);
    expect(screen.getByRole('alert')).toHaveTextContent('Save failed');
    act(() => vi.advanceTimersByTime(30000));
    expect(onDismiss).not.toHaveBeenCalled();
  });

  it('auto-dismisses success messages without restarting on re-render', () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    const { rerender } = render(<Banner tone="success" onDismiss={() => onDismiss()}>Saved</Banner>);
    expect(screen.getByRole('status')).toHaveTextContent('Saved');

    act(() => vi.advanceTimersByTime(4000));
    rerender(<Banner tone="success" onDismiss={() => onDismiss()}>Saved</Banner>);
    act(() => vi.advanceTimersByTime(2500));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });
});
