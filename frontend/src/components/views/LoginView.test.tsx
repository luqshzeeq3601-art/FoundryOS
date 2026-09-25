import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginView } from './LoginView';

const login = vi.fn();
const changePassword = vi.fn();
let mockUser: { mustChangePassword: boolean } | null = null;

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({ login, changePassword, user: mockUser }),
}));

describe('LoginView', () => {
  beforeEach(() => {
    login.mockReset();
    changePassword.mockReset();
    mockUser = null;
  });

  it('starts with empty, labelled credential fields', () => {
    render(<LoginView />);
    const email = screen.getByLabelText('Email');
    const password = screen.getByLabelText('Password');
    expect(email).toHaveValue('');
    expect(password).toHaveValue('');
    expect(email).toHaveAttribute('autocomplete', 'username');
    expect(password).toHaveAttribute('autocomplete', 'current-password');
  });

  it('toggles password visibility', async () => {
    render(<LoginView />);
    const password = screen.getByLabelText('Password');
    expect(password).toHaveAttribute('type', 'password');
    await userEvent.click(screen.getByRole('button', { name: 'Show password' }));
    expect(password).toHaveAttribute('type', 'text');
  });

  it('announces a failed sign-in', async () => {
    login.mockRejectedValue({ response: { status: 401, data: { error: { message: 'Invalid credentials' } } } });
    render(<LoginView />);
    await userEvent.type(screen.getByLabelText('Email'), 'op@plant.local');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong-pass');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid credentials');
    expect(screen.getByLabelText('Password')).toHaveAttribute('aria-invalid', 'true');
  });

  it('validates the new password before calling the API', async () => {
    mockUser = { mustChangePassword: true };
    render(<LoginView />);
    await userEvent.type(screen.getByLabelText('New password'), 'short');
    await userEvent.type(screen.getByLabelText('Confirm new password'), 'short');
    await userEvent.click(screen.getByRole('button', { name: 'Save password and continue' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('at least 8 characters');
    expect(changePassword).not.toHaveBeenCalled();
  });
});
