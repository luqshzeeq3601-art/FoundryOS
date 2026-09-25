import { describe, expect, it } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useHashTab } from './useHashTab';
import { canAccessTab } from '../components/common/Navigation';

describe('useHashTab', () => {
  it('reads the tab from the hash and follows hash changes', () => {
    window.location.hash = '#/machines';
    const { result } = renderHook(() => useHashTab());
    expect(result.current).toBe('machines');

    act(() => {
      window.location.hash = '#/downtime';
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(result.current).toBe('downtime');
  });

  it('falls back for unknown hashes', () => {
    window.location.hash = '#/not-a-page';
    const { result } = renderHook(() => useHashTab('dashboard'));
    expect(result.current).toBe('dashboard');
  });
});

describe('canAccessTab', () => {
  const as = (role: string) => (...roles: string[]) => roles.includes(role);

  it('blocks role-restricted tabs reached by URL', () => {
    expect(canAccessTab('admin', as('OPERATOR'))).toBe(false);
    expect(canAccessTab('admin', as('ADMIN'))).toBe(true);
  });

  it('allows unrestricted tabs for every role', () => {
    expect(canAccessTab('dashboard', as('OPERATOR'))).toBe(true);
  });
});
