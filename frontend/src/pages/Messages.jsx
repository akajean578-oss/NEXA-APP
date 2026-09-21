import { useEffect, useRef, useState } from 'react';
import client from '../api/client';
import { connectRealtime } from '../realtime';
import { useAuth } from '../context/AuthContext.jsx';

export default function Messages() {
  const { user } = useAuth();
  const [conversations, setConversations] = useState([]);
  const [activePartner, setActivePartner] = useState(null);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [typingFrom, setTypingFrom] = useState(null);
  const stompRef = useRef(null);
  const bottomRef = useRef(null);

  async function loadConversations() {
    const res = await client.get('/messages');
    setConversations(res.data);
  }

  async function openConversation(partner) {
    setActivePartner(partner);
    const res = await client.get(`/messages/${partner.userId}`);
    setMessages(res.data);
  }

  useEffect(() => {
    loadConversations();
    const stomp = connectRealtime((c) => {
      stompRef.current = c;
      c.subscribe('/user/queue/messages', (msg) => {
        const dto = JSON.parse(msg.body);
        setMessages((prev) => {
          const relevant = activePartnerRef.current &&
            (dto.senderId === activePartnerRef.current.userId || dto.recipientId === activePartnerRef.current.userId);
          return relevant ? [...prev, dto] : prev;
        });
        loadConversations();
      });
      c.subscribe('/user/queue/typing', (msg) => {
        const data = JSON.parse(msg.body);
        setTypingFrom(data.fromName);
        setTimeout(() => setTypingFrom(null), 2000);
      });
    });
    stompRef.current = stomp;
  }, []);

  // Garde une référence à jour de la conversation active pour le callback de souscription ci-dessus
  const activePartnerRef = useRef(null);
  useEffect(() => { activePartnerRef.current = activePartner; }, [activePartner]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  function sendMessage() {
    if (!text.trim() || !activePartner || !stompRef.current) return;
    stompRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ recipientId: activePartner.userId, text: text.trim() }),
    });
    setText('');
  }

  function notifyTyping() {
    if (!activePartner || !stompRef.current) return;
    stompRef.current.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ recipientId: activePartner.userId, text: '' }),
    });
  }

  return (
    <div className="card" style={{ display: 'flex', height: '70vh', padding: 0, overflow: 'hidden' }}>
      <div style={{ width: 220, borderRight: '1px solid #f5ddcf', overflowY: 'auto' }}>
        {conversations.length === 0 && (
          <p style={{ padding: 14, fontSize: 13, color: '#a08978' }}>
            Aucune conversation. Ajoutez un ami puis écrivez-lui depuis son profil.
          </p>
        )}
        {conversations.map((c) => (
          <div key={c.userId}
               onClick={() => openConversation(c)}
               style={{
                 display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', cursor: 'pointer',
                 background: activePartner?.userId === c.userId ? '#fdf3ec' : 'transparent'
               }}>
            <div className="avatar"></div>
            <div style={{ overflow: 'hidden' }}>
              <div style={{ fontWeight: 600, fontSize: 13 }}>{c.userFullName}</div>
              <div style={{ fontSize: 12, color: '#a08978', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {c.lastMessage}
              </div>
            </div>
            {c.unreadCount > 0 && (
              <span style={{ marginLeft: 'auto', background: '#e8501f', color: '#fff', borderRadius: 10,
                             fontSize: 11, padding: '1px 6px' }}>{c.unreadCount}</span>
            )}
          </div>
        ))}
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {!activePartner ? (
          <div style={{ margin: 'auto', color: '#a08978', fontSize: 14 }}>Sélectionnez une conversation.</div>
        ) : (
          <>
            <div style={{ padding: '10px 14px', borderBottom: '1px solid #f5ddcf', fontWeight: 600 }}>
              {activePartner.userFullName}
              {typingFrom && <span style={{ fontWeight: 400, fontSize: 12, color: '#a08978' }}> — en train d'écrire...</span>}
            </div>
            <div style={{ flex: 1, overflowY: 'auto', padding: 14, display: 'flex', flexDirection: 'column', gap: 6 }}>
              {messages.map((m) => (
                <div key={m.id} style={{
                  alignSelf: m.senderId === user.id ? 'flex-end' : 'flex-start',
                  background: m.senderId === user.id ? '#e8501f' : '#fdf3ec',
                  color: m.senderId === user.id ? '#fff' : '#3a2a1f',
                  borderRadius: 14, padding: '7px 12px', maxWidth: '70%', fontSize: 13
                }}>
                  {m.text}
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
            <div style={{ display: 'flex', gap: 8, padding: 10, borderTop: '1px solid #f5ddcf' }}>
              <input
                className="comment-input"
                placeholder="Écrire un message..."
                value={text}
                onChange={(e) => { setText(e.target.value); notifyTyping(); }}
                onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
              />
              <button className="publish-btn active" onClick={sendMessage}>Envoyer</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
