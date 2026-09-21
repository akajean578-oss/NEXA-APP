import { useEffect, useState } from 'react';
import client from '../api/client';

export default function Friends() {
  const [friends, setFriends] = useState([]);
  const [requests, setRequests] = useState([]);

  async function load() {
    const [f, r] = await Promise.all([client.get('/friends'), client.get('/friends/requests')]);
    setFriends(f.data);
    setRequests(r.data);
  }

  useEffect(() => { load(); }, []);

  async function respond(friendshipId, accept) {
    await client.post(`/friends/${friendshipId}/${accept ? 'accept' : 'decline'}`);
    load();
  }

  return (
    <div>
      {requests.length > 0 && (
        <div className="card">
          <h3 style={{ marginBottom: 10 }}>Demandes d'ami</h3>
          {requests.map((r) => (
            <div key={r.friendshipId} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0' }}>
              <div className="avatar"></div>
              <span style={{ flex: 1 }}>{r.fromUserName}</span>
              <button className="publish-btn active" style={{ padding: '6px 14px' }}
                      onClick={() => respond(r.friendshipId, true)}>Accepter</button>
              <button className="publish-btn" style={{ padding: '6px 14px' }}
                      onClick={() => respond(r.friendshipId, false)}>Refuser</button>
            </div>
          ))}
        </div>
      )}
      <div className="card">
        <h3 style={{ marginBottom: 10 }}>Mes amis</h3>
        {friends.length === 0 && <p style={{ color: '#a08978', fontSize: 13 }}>Aucun ami pour le moment.</p>}
        {friends.map((f) => (
          <div key={f.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0' }}>
            <div className="avatar"></div>
            <span>{f.fullName}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
