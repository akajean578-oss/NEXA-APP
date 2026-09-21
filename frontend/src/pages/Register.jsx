import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Register() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { register } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      await register(email, password, fullName);
      navigate('/');
    } catch (err) {
      setError(err.response?.data || 'Inscription impossible.');
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="logo" style={{ margin: '0 auto 14px' }}>N</div>
        <h2>Créer un compte Nexa</h2>
        {error && <p className="auth-error">{error}</p>}
        <input type="text" placeholder="Nom complet" value={fullName}
               onChange={(e) => setFullName(e.target.value)} required />
        <input type="email" placeholder="Adresse e-mail" value={email}
               onChange={(e) => setEmail(e.target.value)} required />
        <input type="password" placeholder="Mot de passe (6 caractères min.)" value={password}
               onChange={(e) => setPassword(e.target.value)} required minLength={6} />
        <button className="publish-btn active" type="submit">S'inscrire</button>
        <p className="auth-switch">Déjà un compte ? <Link to="/login">Se connecter</Link></p>
      </form>
    </div>
  );
}
