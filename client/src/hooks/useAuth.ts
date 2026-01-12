import { useState, useEffect, useCallback } from 'react';
import { authApi } from '../services/api';
import { AuthStatus } from '../types';

export function useAuth() {
  const [status, setStatus] = useState<AuthStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const checkStatus = useCallback(async () => {
    try {
      setLoading(true);
      const response = await authApi.getStatus();
      if (response.success && response.data) {
        setStatus(response.data);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError('Failed to check authentication status');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkStatus();
  }, [checkStatus]);

  const setup = async (masterPassword: string): Promise<boolean> => {
    try {
      setError(null);
      const response = await authApi.setup(masterPassword);
      if (response.success) {
        await checkStatus();
        return true;
      }
      setError(response.message);
      return false;
    } catch (err) {
      setError('Setup failed');
      return false;
    }
  };

  const login = async (masterPassword: string): Promise<boolean> => {
    try {
      setError(null);
      const response = await authApi.login(masterPassword);
      if (response.success && response.data?.authenticated) {
        setStatus((prev) => (prev ? { ...prev, authenticated: true } : null));
        return true;
      }
      setError(response.message);
      return false;
    } catch (err) {
      setError('Login failed');
      return false;
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await authApi.logout();
      setStatus((prev) => (prev ? { ...prev, authenticated: false } : null));
    } catch (err) {
      setError('Logout failed');
    }
  };

  return {
    status,
    loading,
    error,
    setup,
    login,
    logout,
    clearError: () => setError(null),
  };
}
