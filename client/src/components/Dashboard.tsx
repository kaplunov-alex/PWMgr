import React, { useState, useEffect } from 'react';
import { useEntries } from '../hooks/useEntries';
import { PasswordEntry, PasswordEntryRequest } from '../types';
import { EntryCard } from './EntryCard';
import { EntryForm } from './EntryForm';

interface Props {
  onLogout: () => void;
}

export function Dashboard({ onLogout }: Props) {
  const {
    entries,
    loading,
    error,
    createEntry,
    updateEntry,
    deleteEntry,
    searchEntries,
    refresh,
    clearError,
  } = useEntries(true);

  const [searchQuery, setSearchQuery] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingEntry, setEditingEntry] = useState<PasswordEntry | undefined>();
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      searchEntries(searchQuery);
    }, 300);
    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const handleAddNew = () => {
    setEditingEntry(undefined);
    setShowForm(true);
  };

  const handleEdit = (entry: PasswordEntry) => {
    setEditingEntry(entry);
    setShowForm(true);
  };

  const handleFormSubmit = async (data: PasswordEntryRequest): Promise<boolean> => {
    if (editingEntry) {
      return updateEntry(editingEntry.id, data);
    }
    return createEntry(data);
  };

  const handleDelete = async (id: number) => {
    const success = await deleteEntry(id);
    if (success) {
      showToast('Entry deleted');
    }
  };

  const handleCopy = async (text: string, type: string) => {
    try {
      await navigator.clipboard.writeText(text);
      showToast(`${type} copied to clipboard`);
    } catch (err) {
      showToast('Failed to copy');
    }
  };

  const showToast = (message: string) => {
    setToast(message);
    setTimeout(() => setToast(null), 2000);
  };

  return (
    <div className="app">
      <header className="header">
        <div className="container header-content">
          <div className="logo">
            <span className="logo-icon">🔐</span>
            Password Manager
          </div>
          <button className="btn btn-secondary" onClick={onLogout}>
            🚪 Lock
          </button>
        </div>
      </header>

      <main className="main">
        <div className="container">
          <div className="toolbar">
            <div className="search-box">
              <input
                type="text"
                className="form-input"
                placeholder="Search entries..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <button className="btn btn-primary" onClick={handleAddNew}>
              + Add Entry
            </button>
          </div>

          {error && (
            <div className="alert alert-error">
              {error}
              <button
                className="btn btn-ghost"
                onClick={clearError}
                style={{ marginLeft: 'auto' }}
              >
                ✕
              </button>
            </div>
          )}

          {loading ? (
            <div className="loading">
              <div className="spinner" />
            </div>
          ) : entries.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">🔑</div>
              <h3>No entries yet</h3>
              <p>Add your first password entry to get started</p>
              <button
                className="btn btn-primary"
                onClick={handleAddNew}
                style={{ marginTop: '1rem' }}
              >
                + Add Entry
              </button>
            </div>
          ) : (
            <div className="entry-list">
              {entries.map((entry) => (
                <EntryCard
                  key={entry.id}
                  entry={entry}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onCopy={handleCopy}
                />
              ))}
            </div>
          )}
        </div>
      </main>

      {showForm && (
        <EntryForm
          entry={editingEntry}
          onSubmit={handleFormSubmit}
          onClose={() => {
            setShowForm(false);
            setEditingEntry(undefined);
          }}
        />
      )}

      {toast && <div className="copied-toast">{toast}</div>}
    </div>
  );
}
