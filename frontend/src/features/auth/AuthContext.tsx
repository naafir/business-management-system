import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserSummary, LoginResponse } from '../../types';
import { api } from '../../services/api';

interface AuthContextType {
  user: UserSummary | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: { username: string; password: string }) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(() => {
    const cached = localStorage.getItem('user_profile');
    return cached ? JSON.parse(cached) : null;
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      api.get<UserSummary>('/auth/me')
        .then(userData => {
          setUser(userData);
          localStorage.setItem('user_profile', JSON.stringify(userData));
        })
        .catch(() => {
          api.setToken(null);
          localStorage.removeItem('user_profile');
          setUser(null);
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, []);

  const login = async (credentials: { username: string; password: string }) => {
    const response = await api.post<LoginResponse>('/auth/login', credentials);
    api.setToken(response.token);
    setUser(response.user);
    localStorage.setItem('user_profile', JSON.stringify(response.user));
  };

  const logout = () => {
    api.setToken(null);
    localStorage.removeItem('user_profile');
    setUser(null);
    window.location.href = '/login';
  };

  const refreshUser = async () => {
    try {
      const userData = await api.get<UserSummary>('/auth/me');
      setUser(userData);
      localStorage.setItem('user_profile', JSON.stringify(userData));
    } catch {
      logout();
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
