import { render, screen } from '@testing-library/react';
import { PasswordStrengthBar } from './PasswordStrengthBar';

describe('PasswordStrengthBar', () => {
  it('returns null for empty password', () => {
    const { container } = render(<PasswordStrengthBar password="" />);
    expect(container.firstChild).toBeNull();
  });

  it('calculates weak password correctly', () => {
    render(<PasswordStrengthBar password="abc" />);
    expect(screen.getByText('Weak')).toBeInTheDocument();
  });

  it('calculates fair password correctly', () => {
    render(<PasswordStrengthBar password="abcdefgh" />);
    expect(screen.getByText('Fair')).toBeInTheDocument();
  });

  it('calculates strong password correctly', () => {
    render(<PasswordStrengthBar password="Abcdefgh123" />);
    expect(screen.getByText('Strong')).toBeInTheDocument();
  });

  it('calculates very strong password correctly', () => {
    render(<PasswordStrengthBar password="Abcd1234!@#$Efgh" />);
    expect(screen.getByText('Very Strong')).toBeInTheDocument();
  });

  it('applies correct CSS class for weak password', () => {
    const { container } = render(<PasswordStrengthBar password="abc" />);
    const strengthFill = container.querySelector('.strength-fill');
    expect(strengthFill).toHaveClass('strength-weak');
  });

  it('applies correct CSS class for very strong password', () => {
    const { container } = render(<PasswordStrengthBar password="Abcd1234!@#$Efgh" />);
    const strengthFill = container.querySelector('.strength-fill');
    expect(strengthFill).toHaveClass('strength-very-strong');
  });

  it('gives points for length >= 8', () => {
    render(<PasswordStrengthBar password="abcdefgh" />);
    expect(screen.queryByText('Weak')).not.toBeInTheDocument();
  });

  it('gives points for length >= 12', () => {
    render(<PasswordStrengthBar password="abcdefghijkl" />);
    const label = screen.getByText(/Fair|Strong|Very Strong/);
    expect(label).toBeInTheDocument();
  });

  it('gives points for uppercase letters', () => {
    render(<PasswordStrengthBar password="Abcdefgh" />);
    expect(screen.queryByText('Weak')).not.toBeInTheDocument();
  });

  it('gives points for lowercase letters', () => {
    render(<PasswordStrengthBar password="abcdefgh" />);
    expect(screen.queryByText('Weak')).not.toBeInTheDocument();
  });

  it('gives points for numbers', () => {
    render(<PasswordStrengthBar password="12345678" />);
    expect(screen.queryByText('Weak')).not.toBeInTheDocument();
  });

  it('gives points for special characters', () => {
    render(<PasswordStrengthBar password="!@#$%^&*" />);
    expect(screen.queryByText('Weak')).not.toBeInTheDocument();
  });
});
