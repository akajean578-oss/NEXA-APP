import { useEffect, useState } from 'react';
import { connectRealtime } from '../realtime';

export default function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const c = connectRealtime((stomp) => {
      stomp.subscribe('/user/queue/notifications', (msg) => {
        const data = JSON.parse(msg.body);
        setNotifications((prev) => [data, ...prev].slice(0, 20));
      });
    });
    return () => {};
  }, []);

  return (
    <div style={{ position: 'relative' }}>
      <div className="header-avatar" style={{ background: '#fdf3ec', cursor: 'pointer' }}
           onClick={() => setOpen((o) => !o)}>
        🔔
        {notifications.length > 0 && (
          <span style={{
            position: 'absolute', top: -2, right: -2, background: '#e8501f', color: '#fff',
            borderRadius: '50%', fontSize: 10, width: 16, height: 16,
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>{notifications.length}</span>
        )}
      </div>
      {open && (
        <div style={{
          position: 'absolute', right: 0, top: 44, width: 280, background: '#fff',
          borderRadius: 10, boxShadow: '0 2px 8px rgba(232,80,31,0.15)', padding: 10, zIndex: 10
        }}>
          {notifications.length === 0 && <p style={{ fontSize: 13, color: '#a08978' }}>Aucune notification.</p>}
          {notifications.map((n, i) => (
            <div key={i} style={{ fontSize: 13, padding: '6px 0', borderBottom: '1px solid #f5ddcf' }}>
              {n.message}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
