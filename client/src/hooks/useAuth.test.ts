import { renderHook, act, waitFor } from '@testing-library/react';
import { useAuth } from './useAuth';
import * as api from '../services/api';

jest.mock('../services/api');

const mockedApi = api as jest.Mocked<typeof api>;

describe('useAuth', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches auth status on mount', async () => {
    mockedApi.authApi.getStatus.mockResolvedValue({
      success: true,
      data: { setupRequired: false, authenticated: true },
      message: ''
    });

    const { result } = renderHook(() => useAuth());

    await waitFor(() => {
      expect(result.current.status).toEqual({
        setupRequired: false,
        authenticated: true
      });
    });

    expect(mockedApi.authApi.getStatus).toHaveBeenCalledTimes(1);
  });

  it('sets loading state correctly', async () => {
    mockedApi.authApi.getStatus.mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    );

    const { result } = renderHook(() => useAuth());

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
  });

  it('handles getStatus error', async () => {
    mockedApi.authApi.getStatus.mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useAuth());

    await waitFor(() => {
      expect(result.current.error).toBe('Failed to connect to server');
      expect(result.current.status).toBeNull();
    });
  });

  describe('setup', () => {
    it('calls setup API and updates status on success', async () => {
      mockedApi.authApi.getStatus.mockResolvedValue({
        success: true,
        data: { setupRequired: true, authenticated: false },
        message: ''
      });

      mockedApi.authApi.setup.mockResolvedValue({
        success: true,
        data: null,
        message: 'Setup successful'
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.status).not.toBeNull();
      });

      await act(async () => {
        await result.current.setup('StrongPassword123');
      });

      expect(mockedApi.authApi.setup).toHaveBeenCalledWith('StrongPassword123');
      expect(result.current.status?.setupRequired).toBe(false);
      expect(result.current.status?.authenticated).toBe(true);
    });

    it('sets error on setup failure', async () => {
      mockedApi.authApi.getStatus.mockResolvedValue({
        success: true,
        data: { setupRequired: true, authenticated: false },
        message: ''
      });

      mockedApi.authApi.setup.mockResolvedValue({
        success: false,
        data: null,
        message: 'Password too weak'
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.status).not.toBeNull();
      });

      await act(async () => {
        await result.current.setup('weak');
      });

      expect(result.current.error).toBe('Password too weak');
    });
  });

  describe('login', () => {
    it('calls login API and updates authenticated status on success', async () => {
      mockedApi.authApi.getStatus.mockResolvedValue({
        success: true,
        data: { setupRequired: false, authenticated: false },
        message: ''
      });

      mockedApi.authApi.login.mockResolvedValue({
        success: true,
        data: null,
        message: 'Login successful'
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.status).not.toBeNull();
      });

      await act(async () => {
        await result.current.login('CorrectPassword');
      });

      expect(mockedApi.authApi.login).toHaveBeenCalledWith('CorrectPassword');
      expect(result.current.status?.authenticated).toBe(true);
    });

    it('sets error on login failure', async () => {
      mockedApi.authApi.getStatus.mockResolvedValue({
        success: true,
        data: { setupRequired: false, authenticated: false },
        message: ''
      });

      mockedApi.authApi.login.mockResolvedValue({
        success: false,
        data: null,
        message: 'Invalid password. 4 attempts remaining'
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.status).not.toBeNull();
      });

      await act(async () => {
        await result.current.login('WrongPassword');
      });

      expect(result.current.error).toBe('Invalid password. 4 attempts remaining');
      expect(result.current.status?.authenticated).toBe(false);
    });
  });

  describe('logout', () => {
    it('calls logout API and updates status', async () => {
      mockedApi.authApi.getStatus.mockResolvedValue({
        success: true,
        data: { setupRequired: false, authenticated: true },
        message: ''
      });

      mockedApi.authApi.logout.mockResolvedValue({
        success: true,
        data: null,
        message: 'Logged out'
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.status?.authenticated).toBe(true);
      });

      await act(async () => {
        await result.current.logout();
      });

      expect(mockedApi.authApi.logout).toHaveBeenCalled();
      expect(result.current.status?.authenticated).toBe(false);
    });
  });

  describe('clearError', () => {
    it('clears error state', async () => {
      mockedApi.authApi.getStatus.mockResolvedValue({
        success: true,
        data: { setupRequired: false, authenticated: false },
        message: ''
      });

      mockedApi.authApi.login.mockResolvedValue({
        success: false,
        data: null,
        message: 'Invalid password'
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.status).not.toBeNull();
      });

      await act(async () => {
        await result.current.login('wrong');
      });

      expect(result.current.error).toBe('Invalid password');

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});
