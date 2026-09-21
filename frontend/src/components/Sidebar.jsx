const ITEMS = [
  { key: 'home', label: 'Accueil' },
  { key: 'profile', label: 'Profil' },
  { key: 'friends', label: 'Amis' },
  { key: 'messages', label: 'Messages' },
  { key: 'groups', label: 'Groupes' },
  { key: 'marketplace', label: 'Marché' },
];

export default function Sidebar({ active, onSelect }) {
  return (
    <div className="sidebar-left">
      {ITEMS.map((item) => (
        <div
          key={item.key}
          className={`side-item ${active === item.key ? 'active' : ''}`}
          onClick={() => onSelect(item.key)}
        >
          <div className={`side-icon ${item.key === 'profile' ? 'profile' : ''}`}></div>
          {item.label}
        </div>
      ))}
    </div>
  );
}
