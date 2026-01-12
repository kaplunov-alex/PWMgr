import React, { useState } from 'react';
import { PasswordEntry, PasswordEntryRequest } from '../types';
import { PasswordStrengthBar } from './PasswordStrengthBar';
import { PasswordGenerator } from './PasswordGenerator';

interface Props {
  entry?: PasswordEntry;
  onSubmit: (data: PasswordEntryRequest) => Promise<boolean>;
  onClose: () => void;
}

export function EntryForm({ entry, onSubmit, onClose }: Props) {
  const [formData, setFormData] = useState<PasswordEntryRequest>({
    siteName: entry?.siteName || '',
    username: entry?.username || '',
    password: entry?.password || '',
    notes: entry?.notes || '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showGenerator, setShowGenerator] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const success = await onSubmit(formData);
    setLoading(false);

    if (success) {
      onClose();
    } else {
      setError('Failed to save entry');
    }
  };

  const handleGeneratedPassword = (password: string) => {
    setFormData({ ...formData, password });
  };

  return (
    <>
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header">
            <h2 className="modal-title">
              {entry ? 'Edit Entry' : 'Add New Entry'}
            </h2>
            <button className="btn btn-ghost" onClick={onClose}>
              ✕
            </button>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              {error && <div className="alert alert-error">{error}</div>}

              <div className="form-group">
                <label className="form-label" htmlFor="siteName">
                  Site Name *
                </label>
                <input
                  id="siteName"
                  type="text"
                  className="form-input"
                  value={formData.siteName}
                  onChange={(e) =>
                    setFormData({ ...formData, siteName: e.target.value })
                  }
                  placeholder="e.g., Google, GitHub"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="username">
                  Username / Email *
                </label>
                <input
                  id="username"
                  type="text"
                  className="form-input"
                  value={formData.username}
                  onChange={(e) =>
                    setFormData({ ...formData, username: e.target.value })
                  }
                  placeholder="your@email.com"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">
                  Password *
                </label>
                <div className="form-input-with-btn">
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    className="form-input"
                    value={formData.password}
                    onChange={(e) =>
                      setFormData({ ...formData, password: e.target.value })
                    }
                    required
                  />
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowPassword(!showPassword)}
                    title={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? '🙈' : '👁️'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowGenerator(true)}
                    title="Generate password"
                  >
                    🎲
                  </button>
                </div>
                <PasswordStrengthBar password={formData.password} />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="notes">
                  Notes
                </label>
                <textarea
                  id="notes"
                  className="form-input"
                  value={formData.notes || ''}
                  onChange={(e) =>
                    setFormData({ ...formData, notes: e.target.value })
                  }
                  placeholder="Additional notes (optional)"
                  rows={3}
                />
              </div>
            </div>

            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
              >
                {loading ? 'Saving...' : entry ? 'Update' : 'Add Entry'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {showGenerator && (
        <PasswordGenerator
          onSelect={handleGeneratedPassword}
          onClose={() => setShowGenerator(false)}
        />
      )}
    </>
  );
}
