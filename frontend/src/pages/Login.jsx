import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(err.response?.data || 'Connexion impossible.');
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="logo" style={{ margin: '0 auto 14px' }}>N</div>
        <h2>Connexion à Nexa</h2>
        {error && <p className="auth-error">{error}</p>}
        <input type="email" placeholder="Adresse e-mail" value={email}
               onChange={(e) => setEmail(e.target.value)} required />
        <input type="password" placeholder="Mot de passe" value={password}
               onChange={(e) => setPassword(e.target.value)} required />
        <button className="publish-btn active" type="submit">Se connecter</button>
        <p className="auth-switch">Pas encore de compte ? <Link to="/register">S'inscrire</Link></p>
      </form>
    </div>
  );
}
