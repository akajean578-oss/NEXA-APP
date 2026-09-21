import { createContext, useContext, useState } from 'react';
import client from '../api/client';
import { connectRealtime, disconnectRealtime } from '../realtime';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('nexa_user');
    return saved ? JSON.parse(saved) : null;
  });

  function saveSession(data) {
    localStorage.setItem('nexa_token', data.token);
    localStorage.setItem('nexa_user', JSON.stringify({
      id: data.userId, fullName: data.fullName, email: data.email
    }));
    setUser({ id: data.userId, fullName: data.fullName, email: data.email });
    connectRealtime();
  }

  async function login(email, password) {
    const res = await client.post('/auth/login', { email, password });
    saveSession(res.data);
  }

  async function register(email, password, fullName) {
    const res = await client.post('/auth/register', { email, password, fullName });
    saveSession(res.data);
  }

  function logout() {
    localStorage.removeItem('nexa_token');
    localStorage.removeItem('nexa_user');
    setUser(null);
    disconnectRealtime();
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
