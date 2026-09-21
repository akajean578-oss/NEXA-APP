import { useAuth } from '../context/AuthContext.jsx';
import NotificationBell from './NotificationBell.jsx';

export default function Header({ search, onSearchChange }) {
  const { user, logout } = useAuth();

  return (
    <header>
      <div className="header-left">
        <div className="logo">N</div>
        <span className="brand-name">Nexa</span>
        <div className="search-wrap">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#a08978" strokeWidth="2">
            <circle cx="11" cy="11" r="7" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input className="search-box" placeholder="Rechercher sur Nexa"
                 value={search} onChange={(e) => onSearchChange(e.target.value)} />
        </div>
      </div>
      <div className="header-right">
        <NotificationBell />
        <span style={{ fontWeight: 600, fontSize: 14, marginRight: 4 }}>{user?.fullName}</span>
        <div className="header-avatar" onClick={logout} title="Se déconnecter"></div>
      </div>
    </header>
  );
}
