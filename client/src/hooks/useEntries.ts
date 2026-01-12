import { useState, useEffect, useCallback } from 'react';
import { entriesApi } from '../services/api';
import { PasswordEntry, PasswordEntryRequest } from '../types';

export function useEntries(isAuthenticated: boolean) {
  const [entries, setEntries] = useState<PasswordEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchEntries = useCallback(async () => {
    if (!isAuthenticated) return;

    try {
      setLoading(true);
      setError(null);
      const response = await entriesApi.getAll();
      if (response.success && response.data) {
        setEntries(response.data);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError('Failed to fetch entries');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchEntries();
  }, [fetchEntries]);

  const createEntry = async (entry: PasswordEntryRequest): Promise<boolean> => {
    try {
      setError(null);
      const response = await entriesApi.create(entry);
      if (response.success && response.data) {
        setEntries((prev) => [...prev, response.data!].sort((a, b) =>
          a.siteName.localeCompare(b.siteName)
        ));
        return true;
      }
      setError(response.message);
      return false;
    } catch (err) {
      setError('Failed to create entry');
      return false;
    }
  };

  const updateEntry = async (
    id: number,
    entry: PasswordEntryRequest
  ): Promise<boolean> => {
    try {
      setError(null);
      const response = await entriesApi.update(id, entry);
      if (response.success && response.data) {
        setEntries((prev) =>
          prev
            .map((e) => (e.id === id ? response.data! : e))
            .sort((a, b) => a.siteName.localeCompare(b.siteName))
        );
        return true;
      }
      setError(response.message);
      return false;
    } catch (err) {
      setError('Failed to update entry');
      return false;
    }
  };

  const deleteEntry = async (id: number): Promise<boolean> => {
    try {
      setError(null);
      const response = await entriesApi.delete(id);
      if (response.success) {
        setEntries((prev) => prev.filter((e) => e.id !== id));
        return true;
      }
      setError(response.message);
      return false;
    } catch (err) {
      setError('Failed to delete entry');
      return false;
    }
  };

  const searchEntries = async (query: string): Promise<void> => {
    if (!query.trim()) {
      await fetchEntries();
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const response = await entriesApi.search(query);
      if (response.success && response.data) {
        setEntries(response.data);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError('Search failed');
    } finally {
      setLoading(false);
    }
  };

  return {
    entries,
    loading,
    error,
    createEntry,
    updateEntry,
    deleteEntry,
    searchEntries,
    refresh: fetchEntries,
    clearError: () => setError(null),
  };
}
