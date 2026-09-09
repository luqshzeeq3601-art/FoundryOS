import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { UserDto, LoginRequest, LoginResponse } from '../types';
import { api, setStoredAccessToken, getStoredAccessToken } from '../services/api-client';

interface AuthContextType {
  user: UserDto | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<UserDto>;
  logout: () => Promise<void>;
  changePassword: (newPassword: string) => Promise<void>;
  refreshUserProfile: () => Promise<void>;
  hasRole: (...roles: string[]) => boolean;
  isAdmin: boolean;
  isProductionManager: boolean;
  isEngineer: boolean;
  isTechnician: boolean;
  isOperator: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchProfile = async () => {
    try {
      const userData = await api.get<UserDto>('/auth/me');
      setUser(userData);
    } catch {
      setUser(null);
      setStoredAccessToken(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const token = getStoredAccessToken();
    if (token) {
      fetchProfile();
    } else {
      setIsLoading(false);
    }

    const handleUnauthorized = () => {
      setUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, []);

  const login = async (credentials: LoginRequest): Promise<UserDto> => {
    const data = await api.post<LoginResponse>('/auth/login', credentials);
    setStoredAccessToken(data.accessToken);
    setUser(data.user);
    return data.user;
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      setStoredAccessToken(null);
      setUser(null);
    }
  };

  const changePassword = async (newPassword: string) => {
    await api.post('/auth/change-password', { newPassword });
    await fetchProfile();
  };

  const refreshUserProfile = async () => {
    await fetchProfile();
  };

  const hasRole = (...roles: string[]): boolean => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  const isAdmin = user?.role === 'ADMIN';
  const isProductionManager = user?.role === 'PRODUCTION_MANAGER';
  const isEngineer = user?.role === 'ENGINEER';
  const isTechnician = user?.role === 'TECHNICIAN';
  const isOperator = user?.role === 'OPERATOR';

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        changePassword,
        refreshUserProfile,
        hasRole,
        isAdmin,
        isProductionManager,
        isEngineer,
        isTechnician,
        isOperator,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
