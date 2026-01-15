import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthForm } from './AuthForm';

describe('AuthForm', () => {
  const mockOnSubmit = jest.fn();
  const mockOnClearError = jest.fn();

  beforeEach(() => {
    mockOnSubmit.mockClear();
    mockOnClearError.mockClear();
  });

  describe('Setup Mode', () => {
    it('renders setup form with password confirmation', () => {
      render(
        <AuthForm
          mode="setup"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      expect(screen.getByText('Create Master Password')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Master Password')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Confirm Password')).toBeInTheDocument();
      expect(screen.getByText('Create Password')).toBeInTheDocument();
    });

    it('validates minimum password length', async () => {
      const user = userEvent.setup();
      render(
        <AuthForm
          mode="setup"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      const confirmInput = screen.getByPlaceholderText('Confirm Password');
      const submitButton = screen.getByText('Create Password');

      await user.type(passwordInput, 'short');
      await user.type(confirmInput, 'short');
      await user.click(submitButton);

      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates passwords match', async () => {
      const user = userEvent.setup();
      render(
        <AuthForm
          mode="setup"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      const confirmInput = screen.getByPlaceholderText('Confirm Password');
      const submitButton = screen.getByText('Create Password');

      await user.type(passwordInput, 'ValidPass123');
      await user.type(confirmInput, 'Different123');
      await user.click(submitButton);

      expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('submits when validation passes', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(true);

      render(
        <AuthForm
          mode="setup"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      const confirmInput = screen.getByPlaceholderText('Confirm Password');
      const submitButton = screen.getByText('Create Password');

      await user.type(passwordInput, 'ValidPassword123');
      await user.type(confirmInput, 'ValidPassword123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith('ValidPassword123');
      });
    });

    it('shows password strength indicator', async () => {
      const user = userEvent.setup();
      render(
        <AuthForm
          mode="setup"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      await user.type(passwordInput, 'StrongPass123!');

      expect(screen.getByText(/Strong|Very Strong/)).toBeInTheDocument();
    });
  });

  describe('Login Mode', () => {
    it('renders login form without confirmation', () => {
      render(
        <AuthForm
          mode="login"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      expect(screen.getByText('Unlock Password Manager')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Master Password')).toBeInTheDocument();
      expect(screen.queryByPlaceholderText('Confirm Password')).not.toBeInTheDocument();
      expect(screen.getByText('Unlock')).toBeInTheDocument();
    });

    it('submits password on form submit', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(true);

      render(
        <AuthForm
          mode="login"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      const submitButton = screen.getByText('Unlock');

      await user.type(passwordInput, 'MyPassword123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith('MyPassword123');
      });
    });

    it('does not show password strength indicator', () => {
      render(
        <AuthForm
          mode="login"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      expect(screen.queryByText(/Weak|Fair|Strong/)).not.toBeInTheDocument();
    });
  });

  describe('Common Features', () => {
    it('toggles password visibility', async () => {
      const user = userEvent.setup();
      render(
        <AuthForm
          mode="login"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password') as HTMLInputElement;
      const toggleButton = screen.getByLabelText(/toggle password visibility/i);

      expect(passwordInput.type).toBe('password');

      await user.click(toggleButton);
      expect(passwordInput.type).toBe('text');

      await user.click(toggleButton);
      expect(passwordInput.type).toBe('password');
    });

    it('displays error message when provided', () => {
      render(
        <AuthForm
          mode="login"
          onSubmit={mockOnSubmit}
          error="Invalid password"
          onClearError={mockOnClearError}
        />
      );

      expect(screen.getByText('Invalid password')).toBeInTheDocument();
    });

    it('calls onClearError when typing in password field', async () => {
      const user = userEvent.setup();
      render(
        <AuthForm
          mode="login"
          onSubmit={mockOnSubmit}
          error="Some error"
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      await user.type(passwordInput, 'a');

      expect(mockOnClearError).toHaveBeenCalled();
    });

    it('disables button during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

      render(
        <AuthForm
          mode="login"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      const submitButton = screen.getByText('Unlock') as HTMLButtonElement;

      await user.type(passwordInput, 'password');
      await user.click(submitButton);

      expect(submitButton).toBeDisabled();
    });

    it('clears local error when input changes', async () => {
      const user = userEvent.setup();
      render(
        <AuthForm
          mode="setup"
          onSubmit={mockOnSubmit}
          error={null}
          onClearError={mockOnClearError}
        />
      );

      const passwordInput = screen.getByPlaceholderText('Master Password');
      const confirmInput = screen.getByPlaceholderText('Confirm Password');
      const submitButton = screen.getByText('Create Password');

      // Trigger validation error
      await user.type(passwordInput, 'short');
      await user.type(confirmInput, 'short');
      await user.click(submitButton);

      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();

      // Type more - error should clear
      await user.type(passwordInput, '12345');

      await waitFor(() => {
        expect(screen.queryByText(/at least 8 characters/i)).not.toBeInTheDocument();
      });
    });
  });
});
