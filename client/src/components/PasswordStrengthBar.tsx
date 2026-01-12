import React from 'react';

interface Props {
  password: string;
}

export function PasswordStrengthBar({ password }: Props) {
  const calculateStrength = (pwd: string): { score: number; label: string } => {
    if (!pwd) return { score: 0, label: '' };

    let score = 0;

    if (pwd.length >= 8) score++;
    if (pwd.length >= 12) score++;
    if (pwd.length >= 16) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/\d/.test(pwd)) score++;
    if (/[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(pwd)) score++;

    let label: string;
    if (score <= 2) {
      label = 'Weak';
    } else if (score <= 4) {
      label = 'Fair';
    } else if (score <= 6) {
      label = 'Strong';
    } else {
      label = 'Very Strong';
    }

    return { score, label };
  };

  const { score, label } = calculateStrength(password);

  const getStrengthClass = () => {
    if (score <= 2) return 'strength-weak';
    if (score <= 4) return 'strength-fair';
    if (score <= 6) return 'strength-strong';
    return 'strength-very-strong';
  };

  if (!password) return null;

  return (
    <div>
      <div className="strength-bar">
        <div className={`strength-fill ${getStrengthClass()}`} />
      </div>
      <div className="strength-label">{label}</div>
    </div>
  );
}
