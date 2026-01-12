import React, { useState } from 'react';
import { PasswordEntry } from '../types';

interface Props {
  entry: PasswordEntry;
  onEdit: (entry: PasswordEntry) => void;
  onDelete: (id: number) => void;
  onCopy: (text: string, type: string) => void;
}

export function EntryCard({ entry, onEdit, onDelete, onCopy }: Props) {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);

  const getInitial = (name: string) => {
    return name.charAt(0).toUpperCase();
  };

  const handleDelete = () => {
    if (showConfirmDelete) {
      onDelete(entry.id);
    } else {
      setShowConfirmDelete(true);
      setTimeout(() => setShowConfirmDelete(false), 3000);
    }
  };

  return (
    <div className="card entry-card">
      <div className="entry-icon">{getInitial(entry.siteName)}</div>

      <div className="entry-info">
        <div className="entry-site">{entry.siteName}</div>
        <div className="entry-username">{entry.username}</div>

        <div className="password-field" style={{ marginTop: '0.5rem' }}>
          <span className="password-text">
            {showPassword ? entry.password : '••••••••••••'}
          </span>
          <button
            className="btn btn-ghost"
            onClick={() => setShowPassword(!showPassword)}
            title={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? '🙈' : '👁️'}
          </button>
          <button
            className="btn btn-ghost"
            onClick={() => onCopy(entry.password, 'Password')}
            title="Copy password"
          >
            📋
          </button>
        </div>

        {entry.notes && (
          <div
            style={{
              fontSize: '0.875rem',
              color: 'var(--text-secondary)',
              marginTop: '0.5rem',
            }}
          >
            {entry.notes}
          </div>
        )}
      </div>

      <div className="entry-actions">
        <button
          className="btn btn-ghost"
          onClick={() => onCopy(entry.username, 'Username')}
          title="Copy username"
        >
          👤
        </button>
        <button
          className="btn btn-ghost"
          onClick={() => onEdit(entry)}
          title="Edit entry"
        >
          ✏️
        </button>
        <button
          className={`btn ${showConfirmDelete ? 'btn-danger' : 'btn-ghost'}`}
          onClick={handleDelete}
          title={showConfirmDelete ? 'Click again to confirm' : 'Delete entry'}
        >
          🗑️
        </button>
      </div>
    </div>
  );
}
