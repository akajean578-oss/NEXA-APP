import { useEffect, useState } from 'react';
import client from '../api/client';
import { connectRealtime } from '../realtime';

function timeAgo(iso) {
  const diffMs = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return "À l'instant";
  if (mins < 60) return `${mins} min`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} h`;
  return `${Math.floor(hours / 24)} j`;
}

export default function Post({ post }) {
  const [liked, setLiked] = useState(post.likedByMe);
  const [likeCount, setLikeCount] = useState(post.likeCount);
  const [commentCount, setCommentCount] = useState(post.commentCount);
  const [showComments, setShowComments] = useState(false);
  const [comments, setComments] = useState(null);
  const [commentText, setCommentText] = useState('');

  // Écoute en direct : si quelqu'un d'autre aime ou commente ce post, on se met à jour sans recharger.
  useEffect(() => {
    let subs = [];
    const stomp = connectRealtime((client) => {
      subs.push(client.subscribe(`/topic/posts/${post.id}/likes`, (msg) => {
        const data = JSON.parse(msg.body);
        setLikeCount(data.likeCount);
      }));
      subs.push(client.subscribe(`/topic/posts/${post.id}/comments`, (msg) => {
        const data = JSON.parse(msg.body);
        setCommentCount((c) => c + 1);
        setComments((prev) => (prev ? [...prev, data] : prev));
      }));
    });
    return () => subs.forEach((s) => s?.unsubscribe?.());
  }, [post.id]);

  async function toggleLike() {
    const res = await client.post(`/posts/${post.id}/like`);
    setLiked(res.data.liked);
  }

  async function openComments() {
    setShowComments((s) => !s);
    if (!comments) {
      const res = await client.get(`/posts/${post.id}/comments`);
      setComments(res.data);
    }
  }

  async function submitComment(e) {
    if (e.key !== 'Enter' || !commentText.trim()) return;
    await client.post(`/posts/${post.id}/comments`, { text: commentText.trim() });
    setCommentText('');
    // Le commentaire arrivera via la souscription WebSocket ci-dessus (temps réel).
  }

  return (
    <div className="card post">
      <div className="post-header">
        <div className="avatar"></div>
        <div>
          <div className="post-user">{post.author.fullName}</div>
          <div className="post-time">{timeAgo(post.createdAt)}</div>
        </div>
      </div>
      {post.text && <div className="post-text">{post.text}</div>}
      {post.imageUrl && (
        <img src={post.imageUrl} alt="" style={{ width: '100%', borderRadius: 10, marginTop: 8, maxHeight: 420, objectFit: 'cover' }} />
      )}
      <div className="post-stats">
        <span className="like-count">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="#e8501f" stroke="#e8501f" strokeWidth="1">
            <path d="M12 22c4-3 8-6.5 8-11a5 5 0 0 0-8-4 5 5 0 0 0-8 4c0 4.5 4 8 8 11z"></path>
          </svg>
          <span>{likeCount}</span>
        </span>
        <span>{commentCount} commentaires</span>
      </div>
      <div className="post-actions">
        <div className={`post-btn ${liked ? 'liked' : ''}`} onClick={toggleLike}>
          J'aime
        </div>
        <div className="post-btn" onClick={openComments}>
          Commenter
        </div>
        <div className="post-btn">Partager</div>
      </div>
      {showComments && (
        <div className="comments show">
          <div className="comment-list">
            {(comments || []).map((c) => (
              <div className="comment-bubble" key={c.id}>
                <b>{c.author.fullName} </b>{c.text}
              </div>
            ))}
          </div>
          <div className="comment-input-row">
            <input
              className="comment-input"
              placeholder="Écrire un commentaire... (Entrée pour valider)"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              onKeyDown={submitComment}
            />
          </div>
        </div>
      )}
    </div>
  );
}
