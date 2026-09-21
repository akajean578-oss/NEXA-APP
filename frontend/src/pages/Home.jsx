import { useEffect, useState } from 'react';
import client from '../api/client';
import { connectRealtime } from '../realtime';
import Header from '../components/Header.jsx';
import Sidebar from '../components/Sidebar.jsx';
import Composer from '../components/Composer.jsx';
import Post from '../components/Post.jsx';
import Messages from './Messages.jsx';
import Friends from './Friends.jsx';

export default function Home() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasNew, setHasNew] = useState(false);
  const [pendingNew, setPendingNew] = useState([]);
  const [search, setSearch] = useState('');
  const [searchResults, setSearchResults] = useState(null);
  const [page, setPage] = useState('home');

  async function loadFeed() {
    setLoading(true);
    const res = await client.get('/posts');
    setPosts(res.data);
    setPendingNew([]);
    setHasNew(false);
    setLoading(false);
  }

  useEffect(() => {
    loadFeed();
    // Nouveaux posts publiés par n'importe qui : signalés via un bandeau "Actualiser"
    // plutôt qu'insérés brutalement, pour ne pas faire sauter le fil pendant la lecture.
    const subs = [];
    connectRealtime((stomp) => {
      subs.push(stomp.subscribe('/topic/feed', (msg) => {
        const newPost = JSON.parse(msg.body);
        setPendingNew((prev) => [newPost, ...prev.filter((p) => p.id !== newPost.id)]);
        setHasNew(true);
      }));
    });
    return () => subs.forEach((s) => s?.unsubscribe?.());
  }, []);

  function showNewPosts() {
    setPosts((prev) => [...pendingNew, ...prev.filter((p) => !pendingNew.some((n) => n.id === p.id))]);
    setPendingNew([]);
    setHasNew(false);
  }

  // Recherche côté serveur, avec un léger anti-rebond pour ne pas spammer l'API à chaque frappe.
  useEffect(() => {
    if (!search.trim()) {
      setSearchResults(null);
      return;
    }
    const timeout = setTimeout(async () => {
      const res = await client.get('/search', { params: { q: search.trim() } });
      setSearchResults(res.data);
    }, 300);
    return () => clearTimeout(timeout);
  }, [search]);

  async function addFriend(userId) {
    await client.post(`/friends/request/${userId}`);
  }

  return (
    <>
      <Header search={search} onSearchChange={setSearch} />
      <div className="layout">
        <Sidebar active={page} onSelect={setPage} />
        <div className="main-content">
          {searchResults ? (
            <div className="page active">
              <div className="card">
                <h3 style={{ marginBottom: 10 }}>Personnes</h3>
                {searchResults.users.length === 0 && <p style={{ color: '#a08978', fontSize: 13 }}>Aucun résultat.</p>}
                {searchResults.users.map((u) => (
                  <div key={u.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 0' }}>
                    <div className="avatar"></div>
                    <span style={{ flex: 1 }}>{u.fullName}</span>
                    <button className="publish-btn active" style={{ padding: '5px 12px', fontSize: 12 }}
                            onClick={() => addFriend(u.id)}>Ajouter</button>
                  </div>
                ))}
              </div>
              <div className="card">
                <h3 style={{ marginBottom: 10 }}>Publications</h3>
                {searchResults.posts.length === 0 && <p style={{ color: '#a08978', fontSize: 13 }}>Aucun résultat.</p>}
                {searchResults.posts.map((p) => (
                  <div key={p.id} style={{ padding: '8px 0', borderBottom: '1px solid #f5ddcf' }}>
                    <b>{p.authorName}</b> — {p.text}
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <>
              {page === 'home' && (
                <div className="page active">
                  <Composer onPosted={loadFeed} />
                  {hasNew && (
                    <button onClick={showNewPosts} className="publish-btn active"
                            style={{ width: '100%', marginBottom: 12 }}>
                      ↑ Nouvelles publications — actualiser le fil
                    </button>
                  )}
                  {loading && <p style={{ textAlign: 'center', color: '#a08978' }}>Chargement du fil...</p>}
                  {!loading && posts.map((post) => <Post key={post.id} post={post} />)}
                  {!loading && posts.length === 0 && (
                    <p style={{ textAlign: 'center', color: '#a08978' }}>Aucune publication pour le moment.</p>
                  )}
                </div>
              )}
              {page === 'messages' && <div className="page active"><Messages /></div>}
              {page === 'friends' && <div className="page active"><Friends /></div>}
              {page !== 'home' && page !== 'messages' && page !== 'friends' && (
                <div className="page-header">
                  <div className="big-avatar"></div>
                  <h2>Page « {page} »</h2>
                  <p>Cette section est prête à être développée (à connecter à sa propre API).</p>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </>
  );
}
