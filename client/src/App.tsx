import React from 'react';
import { useAuth } from './hooks/useAuth';
import { AuthForm } from './components/AuthForm';
import { Dashboard } from './components/Dashboard';
import './index.css';

function App() {
  const { status, loading, error, setup, login, logout, clearError } = useAuth();

  if (loading) {
    return (
      <div className="auth-page">
        <div className="loading">
          <div className="spinner" />
        </div>
      </div>
    );
  }

  if (!status) {
    return (
      <div className="auth-page">
        <div className="card auth-card">
          <div className="card-body">
            <div className="alert alert-error">
              Unable to connect to server. Please ensure the backend is running.
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (status.setupRequired) {
    return (
      <AuthForm
        mode="setup"
        onSubmit={setup}
        error={error}
        onClearError={clearError}
      />
    );
  }

  if (!status.authenticated) {
    return (
      <AuthForm
        mode="login"
        onSubmit={login}
        error={error}
        onClearError={clearError}
      />
    );
  }

  return <Dashboard onLogout={logout} />;
}

export default App;
