import React, { useId, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { IndustrialButton } from '../common/IndustrialButton';
import { ShieldAlert, KeyRound, Eye, EyeOff } from 'lucide-react';

const MIN_PASSWORD_LENGTH = 8;

const inputClass =
  'w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-industrial-300 aria-[invalid=true]:border-hazard-red';

interface PasswordFieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete: 'current-password' | 'new-password';
  describedBy?: string;
  invalid?: boolean;
}

const PasswordField: React.FC<PasswordFieldProps> = ({ id, label, value, onChange, autoComplete, describedBy, invalid }) => {
  const [visible, setVisible] = useState(false);
  return (
    <div>
      <label htmlFor={id} className="block text-xs font-mono uppercase text-industrial-300 mb-1">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          required
          autoComplete={autoComplete}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          className={`${inputClass} pr-12`}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Hide password' : 'Show password'}
          aria-pressed={visible}
          className="absolute inset-y-0 right-0 w-11 flex items-center justify-center text-industrial-400 hover:text-white"
        >
          {visible ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
        </button>
      </div>
    </div>
  );
};

export const LoginView: React.FC = () => {
  const { login, user, changePassword } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const ids = {
    email: useId(),
    password: useId(),
    newPassword: useId(),
    confirmPassword: useId(),
    rules: useId(),
    error: useId(),
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage(null);
    try {
      await login({ email, password });
    } catch (err: any) {
      const apiMessage = err.response?.data?.error?.message;
      const serviceUnavailable = !apiMessage && (!err.response || err.response.status >= 500);
      setErrorMessage(
        serviceUnavailable
          ? 'The FoundryOS server is not responding. Try again in a moment or contact your administrator.'
          : apiMessage || 'Email or password is incorrect.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      setErrorMessage(`Use at least ${MIN_PASSWORD_LENGTH} characters.`);
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMessage('The two passwords do not match.');
      return;
    }
    setIsLoading(true);
    setErrorMessage(null);
    try {
      await changePassword(newPassword);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.error?.message || 'Password could not be saved. Try again.');
    } finally {
      setIsLoading(false);
    }
  };

  // Authenticated with a temporary password: force a change before entering the app.
  if (user && user.mustChangePassword) {
    return (
      <main className="min-h-screen bg-substrate-dark flex items-center justify-center p-4">
        <div className="w-full max-w-md bg-substrate-card border-2 border-hazard-amber p-6 sm:p-8 relative">
          <div className="h-1.5 w-full bg-hazard-stripes-amber absolute top-0 left-0" aria-hidden="true" />

          <div className="flex items-center gap-3 mb-4 mt-2">
            <KeyRound className="text-hazard-amber shrink-0" size={28} aria-hidden="true" />
            <h1 className="text-xl font-bold font-mono uppercase text-white">Set a new password</h1>
          </div>

          <p className="text-sm text-industrial-300 mb-6 leading-relaxed">
            You signed in with a temporary password. Choose a permanent one to continue.
          </p>

          {errorMessage && (
            <div id={ids.error} role="alert" className="mb-4 p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono">
              {errorMessage}
            </div>
          )}

          <form onSubmit={handlePasswordChange} className="space-y-4" noValidate>
            <PasswordField
              id={ids.newPassword}
              label="New password"
              value={newPassword}
              onChange={setNewPassword}
              autoComplete="new-password"
              describedBy={errorMessage ? `${ids.rules} ${ids.error}` : ids.rules}
              invalid={!!errorMessage}
            />
            <p id={ids.rules} className="text-xs text-industrial-400 -mt-2">
              At least {MIN_PASSWORD_LENGTH} characters.
            </p>

            <PasswordField
              id={ids.confirmPassword}
              label="Confirm new password"
              value={confirmPassword}
              onChange={setConfirmPassword}
              autoComplete="new-password"
              describedBy={errorMessage ? ids.error : undefined}
              invalid={!!errorMessage}
            />

            <IndustrialButton type="submit" variant="warning" size="lg" isLoading={isLoading} className="w-full mt-4">
              Save password and continue
            </IndustrialButton>
          </form>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-substrate-dark flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-md bg-substrate-card border-2 border-industrial-700 p-6 sm:p-8 relative">
        <div className="h-1.5 w-full bg-hazard-stripes absolute top-0 left-0" aria-hidden="true" />

        <div className="flex items-center gap-3 mb-6 mt-2">
          <div
            className="w-9 h-9 bg-hazard-red text-black font-black text-xl flex items-center justify-center shrink-0"
            aria-hidden="true"
          >
            F
          </div>
          <h1 className="text-xl font-extrabold font-mono uppercase text-white tracking-wider">
            FOUNDRY<span className="text-hazard-red">//</span>OS
          </h1>
        </div>

        {errorMessage && (
          <div
            id={ids.error}
            role="alert"
            className="mb-4 p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono flex items-start gap-2"
          >
            <ShieldAlert size={16} className="shrink-0 mt-0.5" aria-hidden="true" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label htmlFor={ids.email} className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Email
            </label>
            <input
              id={ids.email}
              type="email"
              required
              autoComplete="username"
              inputMode="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              aria-describedby={errorMessage ? ids.error : undefined}
              aria-invalid={errorMessage ? true : undefined}
              className={inputClass}
            />
          </div>

          <PasswordField
            id={ids.password}
            label="Password"
            value={password}
            onChange={setPassword}
            autoComplete="current-password"
            describedBy={errorMessage ? ids.error : undefined}
            invalid={!!errorMessage}
          />

          <IndustrialButton type="submit" variant="hazard" size="lg" isLoading={isLoading} className="w-full mt-6">
            Sign in
          </IndustrialButton>
        </form>

        <p className="mt-6 pt-4 border-t border-substrate-border text-center text-xs text-industrial-400">
          Forgot your password? Ask a plant administrator to reset it.
        </p>
      </div>
    </main>
  );
};
