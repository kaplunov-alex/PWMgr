import React, { useState } from 'react';
import { generatorApi } from '../services/api';
import { GenerateOptions } from '../types';
import { PasswordStrengthBar } from './PasswordStrengthBar';

interface Props {
  onSelect: (password: string) => void;
  onClose: () => void;
}

export function PasswordGenerator({ onSelect, onClose }: Props) {
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [options, setOptions] = useState<GenerateOptions>({
    length: 16,
    uppercase: true,
    lowercase: true,
    numbers: true,
    special: true,
  });

  const generatePassword = async () => {
    setLoading(true);
    try {
      const response = await generatorApi.generate(options);
      if (response.success && response.data) {
        setPassword(response.data.password);
      }
    } catch (err) {
      console.error('Failed to generate password:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleUsePassword = () => {
    if (password) {
      onSelect(password);
      onClose();
    }
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(password);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Generate Password</h2>
          <button className="btn btn-ghost" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="modal-body">
          <div className="form-group">
            <label className="form-label">Length: {options.length}</label>
            <input
              type="range"
              min="8"
              max="64"
              value={options.length}
              onChange={(e) =>
                setOptions({ ...options, length: parseInt(e.target.value) })
              }
              className="form-input"
              style={{ padding: 0 }}
            />
          </div>

          <div className="generator-options">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={options.uppercase}
                onChange={(e) =>
                  setOptions({ ...options, uppercase: e.target.checked })
                }
              />
              Uppercase (A-Z)
            </label>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={options.lowercase}
                onChange={(e) =>
                  setOptions({ ...options, lowercase: e.target.checked })
                }
              />
              Lowercase (a-z)
            </label>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={options.numbers}
                onChange={(e) =>
                  setOptions({ ...options, numbers: e.target.checked })
                }
              />
              Numbers (0-9)
            </label>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={options.special}
                onChange={(e) =>
                  setOptions({ ...options, special: e.target.checked })
                }
              />
              Special (!@#$%)
            </label>
          </div>

          <button
            className="btn btn-primary"
            onClick={generatePassword}
            disabled={loading}
            style={{ width: '100%', marginBottom: '1rem' }}
          >
            {loading ? 'Generating...' : 'Generate Password'}
          </button>

          {password && (
            <div>
              <div className="password-field">
                <span className="password-text" style={{ fontFamily: 'monospace' }}>
                  {password}
                </span>
                <button
                  className="btn btn-ghost"
                  onClick={copyToClipboard}
                  title="Copy to clipboard"
                >
                  📋
                </button>
              </div>
              <PasswordStrengthBar password={password} />
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={handleUsePassword}
            disabled={!password}
          >
            Use Password
          </button>
        </div>
      </div>
    </div>
  );
}
