import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { IndustrialButton } from '../common/IndustrialButton';
import { ShieldAlert, KeyRound, Lock, Mail } from 'lucide-react';

export const LoginView: React.FC = () => {
  const { login, user, changePassword } = useAuth();
  const [email, setEmail] = useState('admin@factoryos.local');
  const [password, setPassword] = useState('AdminBootstrap2026!Secure');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage(null);
    try {
      await login({ email, password });
    } catch (err: any) {
      const msg = err.response?.data?.error?.message || 'Authentication failed. Check credentials and retry.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return;
    }
    if (newPassword.length < 8) {
      setErrorMessage('Password must be at least 8 characters long.');
      return;
    }
    setIsLoading(true);
    setErrorMessage(null);
    try {
      await changePassword(newPassword);
    } catch (err: any) {
      const msg = err.response?.data?.error?.message || 'Password update failed.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  // If user is authenticated but MUST change temporary password
  if (user && user.mustChangePassword) {
    return (
      <div className="min-h-screen bg-substrate-dark flex items-center justify-center p-4">
        <div className="w-full max-w-md bg-substrate-card border-2 border-hazard-amber p-6 sm:p-8 shadow-2xl relative">
          <div className="h-1.5 w-full bg-hazard-stripes-amber absolute top-0 left-0" />
          
          <div className="flex items-center gap-3 mb-6 mt-2">
            <KeyRound className="text-hazard-amber" size={28} />
            <div>
              <div className="text-xs font-mono text-industrial-500 uppercase tracking-widest">[ SECURITY POLICY // REQUIRED ]</div>
              <h1 className="text-xl font-bold font-mono uppercase text-white">SET NEW PASSWORD</h1>
            </div>
          </div>

          <p className="text-xs font-mono text-industrial-400 mb-6 leading-relaxed">
            Your account is currently using a temporary password. You must establish a new secure permanent password before accessing plant telemetry.
          </p>

          {errorMessage && (
            <div className="mb-4 p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono">
              [ ERROR ]: {errorMessage}
            </div>
          )}

          <form onSubmit={handlePasswordChange} className="space-y-4">
            <div>
              <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
                New Permanent Password
              </label>
              <input
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-hazard-amber"
                placeholder="••••••••••••"
              />
            </div>

            <div>
              <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
                Confirm Permanent Password
              </label>
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-hazard-amber"
                placeholder="••••••••••••"
              />
            </div>

            <IndustrialButton
              type="submit"
              variant="warning"
              size="lg"
              isLoading={isLoading}
              className="w-full mt-4"
            >
              SAVE PASSWORD & ENTER SYSTEM
            </IndustrialButton>
          </form>
        </div>
      </div>
    );
  }

  // Standard Login Screen
  return (
    <div className="min-h-screen bg-substrate-dark flex flex-col items-center justify-center p-4 relative">
      {/* Background Decorative Grid Markers */}
      <div className="absolute top-6 left-6 text-xs text-industrial-700 font-mono">
        FACTORY//OS :: NODE-PLANT-01
      </div>
      <div className="absolute bottom-6 right-6 text-xs text-industrial-700 font-mono">
        BUILD 2026.09 // LATENCY &lt; 20MS
      </div>

      <div className="w-full max-w-md bg-substrate-card border-2 border-industrial-700 p-6 sm:p-8 shadow-2xl relative">
        <div className="h-1.5 w-full bg-hazard-stripes absolute top-0 left-0" />

        {/* Header */}
        <div className="flex items-center gap-3 mb-6 mt-2">
          <div className="w-9 h-9 bg-hazard-red text-black font-black text-xl flex items-center justify-center shrink-0">
            F
          </div>
          <div>
            <div className="text-xs font-mono text-industrial-500 uppercase tracking-widest">[ AUTH // TERMINAL ]</div>
            <h1 className="text-xl font-extrabold font-mono uppercase text-white tracking-wider">
              FACTORY<span className="text-hazard-red">//</span>OS
            </h1>
          </div>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono flex items-start gap-2">
            <ShieldAlert size={16} className="shrink-0 mt-0.5" />
            <div>
              <span className="font-bold">[ ACCESS DENIED ]: </span>
              {errorMessage}
            </div>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1 flex items-center gap-1.5">
              <Mail size={12} />
              <span>Operator Email Address</span>
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
              placeholder="operator@factoryos.local"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1 flex items-center gap-1.5">
              <Lock size={12} />
              <span>Security Access Key</span>
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
              placeholder="••••••••••••"
            />
          </div>

          <IndustrialButton
            type="submit"
            variant="hazard"
            size="lg"
            isLoading={isLoading}
            className="w-full mt-6"
          >
            AUTHORIZE & INITIALIZE SESSION
          </IndustrialButton>
        </form>

        <div className="mt-6 pt-4 border-t border-substrate-border text-center">
          <span className="text-[11px] font-mono text-industrial-500 uppercase">
            RESTRICTED TERMINAL — SINGLE PLANT DEPLOYMENT
          </span>
        </div>
      </div>
    </div>
  );
};
