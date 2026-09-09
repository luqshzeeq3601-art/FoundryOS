import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { UserDto, LoginRequest, LoginResponse, PlantDto } from '../types';
import { 
  api, 
  setStoredAccessToken, 
  getStoredAccessToken, 
  setStoredActivePlantId, 
  getStoredActivePlantId,
  hierarchyApi,
  authApi
} from '../services/api-client';

interface AuthContextType {
  user: UserDto | null;
  activePlant: PlantDto | null;
  authorizedPlants: PlantDto[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<UserDto>;
  logout: () => Promise<void>;
  switchPlant: (plantId: string) => Promise<void>;
  refreshAuthorizedPlants: () => Promise<void>;
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
  const [activePlant, setActivePlant] = useState<PlantDto | null>(null);
  const [authorizedPlants, setAuthorizedPlants] = useState<PlantDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchPlants = useCallback(async () => {
    try {
      const plants = await hierarchyApi.getAuthorizedPlants();
      setAuthorizedPlants(plants);

      const storedPlantId = getStoredActivePlantId();
      if (storedPlantId) {
        const found = plants.find(p => p.id === storedPlantId);
        if (found) {
          setActivePlant(found);
          return;
        }
      }

      // Default to first authorized plant if available
      if (plants.length > 0) {
        setActivePlant(plants[0]);
        setStoredActivePlantId(plants[0].id);
      } else {
        setActivePlant(null);
      }
    } catch (err) {
      console.warn('Failed to fetch authorized plants:', err);
      setAuthorizedPlants([]);
      setActivePlant(null);
    }
  }, []);

  const fetchProfile = useCallback(async () => {
    try {
      const userData = await api.get<UserDto>('/auth/me');
      setUser(userData);
      await fetchPlants();
    } catch {
      setUser(null);
      setActivePlant(null);
      setAuthorizedPlants([]);
      setStoredAccessToken(null);
      setStoredActivePlantId(null);
    } finally {
      setIsLoading(false);
    }
  }, [fetchPlants]);

  useEffect(() => {
    const token = getStoredAccessToken();
    if (token) {
      fetchProfile();
    } else {
      setIsLoading(false);
    }

    const handleUnauthorized = () => {
      setUser(null);
      setActivePlant(null);
      setAuthorizedPlants([]);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [fetchProfile]);

  const login = async (credentials: LoginRequest): Promise<UserDto> => {
    const data = await api.post<LoginResponse>('/auth/login', credentials);
    setStoredAccessToken(data.accessToken);
    setUser(data.user);
    await fetchPlants();
    return data.user;
  };

  const switchPlant = async (plantId: string): Promise<void> => {
    const data = await authApi.switchPlant(plantId);
    setStoredAccessToken(data.accessToken);
    setStoredActivePlantId(plantId);
    setUser(data.user);

    const found = authorizedPlants.find(p => p.id === plantId);
    if (found) {
      setActivePlant(found);
    } else {
      await fetchPlants();
    }
  };

  const refreshAuthorizedPlants = async () => {
    await fetchPlants();
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      setStoredAccessToken(null);
      setStoredActivePlantId(null);
      setUser(null);
      setActivePlant(null);
      setAuthorizedPlants([]);
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
        activePlant,
        authorizedPlants,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        switchPlant,
        refreshAuthorizedPlants,
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

