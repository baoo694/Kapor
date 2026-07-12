import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from '../components/Card';
import { Input } from '../components/Input';
import { Button } from '../components/Button';
import { api } from '../services/api';

export const Login: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await api.login({ email, password });
      
      // Store tokens and potentially user role
      if (data.accessToken) {
        localStorage.setItem('kapor_admin_token', data.accessToken);
        if (data.refreshToken) {
          localStorage.setItem('kapor_admin_refresh_token', data.refreshToken);
        }
        navigate('/');
      } else {
        setError('Invalid response from server.');
      }
    } catch (err: any) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center w-full" style={{ height: '100vh' }}>
      <Card className="w-full" style={{ maxWidth: '400px' }}>
        <h2 className="mb-lg" style={{ textAlign: 'center' }}>Kapor Admin</h2>
        
        {error && (
          <div className="mb-md p-sm" style={{ background: 'rgba(239, 68, 68, 0.1)', color: 'var(--color-destructive)', borderRadius: '8px', padding: '12px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleLogin} className="flex-col gap-md">
          <Input 
            label="Email Address" 
            type="email" 
            value={email}
            onChange={e => setEmail(e.target.value)}
            placeholder="admin@kapor.com" 
            required 
            disabled={loading}
          />
          <Input 
            label="Password" 
            type="password" 
            value={password}
            onChange={e => setPassword(e.target.value)}
            placeholder="••••••••" 
            required 
            disabled={loading}
          />
          <Button type="submit" className="w-full mt-sm" disabled={loading}>
            {loading ? 'Logging in...' : 'Login to Dashboard'}
          </Button>
        </form>
      </Card>
    </div>
  );
};
