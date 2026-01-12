export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface PasswordEntry {
  id: number;
  siteName: string;
  username: string;
  password: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PasswordEntryRequest {
  siteName: string;
  username: string;
  password: string;
  notes?: string;
}

export interface AuthStatus {
  setupRequired: boolean;
  authenticated: boolean;
}

export interface PasswordStrength {
  score: number;
  maxScore: number;
  label: 'Weak' | 'Fair' | 'Strong' | 'Very Strong';
}

export interface GeneratedPassword {
  password: string;
  strength: PasswordStrength;
}

export interface GenerateOptions {
  length: number;
  uppercase: boolean;
  lowercase: boolean;
  numbers: boolean;
  special: boolean;
}
