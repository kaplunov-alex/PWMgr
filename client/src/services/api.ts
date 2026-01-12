import {
  ApiResponse,
  AuthStatus,
  PasswordEntry,
  PasswordEntryRequest,
  GeneratedPassword,
  GenerateOptions,
} from '../types';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  const data = await response.json();
  return data;
}

export const authApi = {
  getStatus: () => request<AuthStatus>('/auth/status'),

  setup: (masterPassword: string) =>
    request<void>('/auth/setup', {
      method: 'POST',
      body: JSON.stringify({ masterPassword }),
    }),

  login: (masterPassword: string) =>
    request<{ authenticated: boolean; remainingAttempts?: number }>(
      '/auth/login',
      {
        method: 'POST',
        body: JSON.stringify({ masterPassword }),
      }
    ),

  logout: () =>
    request<void>('/auth/logout', {
      method: 'POST',
    }),
};

export const entriesApi = {
  getAll: () => request<PasswordEntry[]>('/entries'),

  get: (id: number) => request<PasswordEntry>(`/entries/${id}`),

  create: (entry: PasswordEntryRequest) =>
    request<PasswordEntry>('/entries', {
      method: 'POST',
      body: JSON.stringify(entry),
    }),

  update: (id: number, entry: PasswordEntryRequest) =>
    request<PasswordEntry>(`/entries/${id}`, {
      method: 'PUT',
      body: JSON.stringify(entry),
    }),

  delete: (id: number) =>
    request<void>(`/entries/${id}`, {
      method: 'DELETE',
    }),

  search: (query: string) =>
    request<PasswordEntry[]>(`/entries/search?q=${encodeURIComponent(query)}`),
};

export const generatorApi = {
  generate: (options: GenerateOptions) => {
    const params = new URLSearchParams({
      length: options.length.toString(),
      uppercase: options.uppercase.toString(),
      lowercase: options.lowercase.toString(),
      numbers: options.numbers.toString(),
      special: options.special.toString(),
    });
    return request<GeneratedPassword>(`/generate?${params}`);
  },
};
