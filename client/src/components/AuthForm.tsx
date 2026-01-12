import React, { useState } from 'react';
import { PasswordStrengthBar } from './PasswordStrengthBar';

interface Props {
  mode: 'setup' | 'login';
  onSubmit: (password: string) => Promise<boolean>;
  error: string | null;
  onClearError: () => void;
}

export function AuthForm({ mode, onSubmit, error, onClearError }: Props) {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    onClearError();

    if (mode === 'setup') {
      if (password.length < 8) {
        setLocalError('Master password must be at least 8 characters');
        return;
      }
      if (password !== confirmPassword) {
        setLocalError('Passwords do not match');
        return;
      }
    }

    setLoading(true);
    await onSubmit(password);
    setLoading(false);
  };

  const displayError = localError || error;

  return (
    <div className="auth-page">
      <div className="card auth-card">
        <div className="auth-header">
          <div className="logo">
            <span className="logo-icon">🔐</span>
            Password Manager
          </div>
          <h1 className="auth-title">
            {mode === 'setup' ? 'Create Master Password' : 'Welcome Back'}
          </h1>
          <p className="auth-subtitle">
            {mode === 'setup'
              ? 'Choose a strong master password to protect your vault'
              : 'Enter your master password to unlock your vault'}
          </p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          {displayError && (
            <div className="alert alert-error">{displayError}</div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="master-password">
              Master Password
            </label>
            <div className="form-input-with-btn">
              <input
                id="master-password"
                type={showPassword ? 'text' : 'password'}
                className="form-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter master password"
                autoFocus
                required
              />
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
            {mode === 'setup' && <PasswordStrengthBar password={password} />}
          </div>

          {mode === 'setup' && (
            <div className="form-group">
              <label className="form-label" htmlFor="confirm-password">
                Confirm Password
              </label>
              <input
                id="confirm-password"
                type={showPassword ? 'text' : 'password'}
                className="form-input"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Confirm master password"
                required
              />
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ width: '100%', marginTop: '1rem' }}
          >
            {loading
              ? mode === 'setup'
                ? 'Creating...'
                : 'Unlocking...'
              : mode === 'setup'
              ? 'Create Vault'
              : 'Unlock Vault'}
          </button>
        </form>

        {mode === 'setup' && (
          <div
            style={{
              padding: '0 1.5rem 1.5rem',
              fontSize: '0.75rem',
              color: 'var(--text-secondary)',
              textAlign: 'center',
            }}
          >
            Your master password cannot be recovered if forgotten. Make sure to
            remember it or store it securely.
          </div>
        )}
      </div>
    </div>
  );
}
