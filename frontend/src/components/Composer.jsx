import { useState } from 'react';
import client from '../api/client';

export default function Composer({ onPosted }) {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState('');
  const [imageUrl, setImageUrl] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleFile(e) {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    setError('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await client.post('/media/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setImageUrl(res.data.url);
    } catch (err) {
      setError(err.response?.data || "Échec de l'envoi de l'image.");
    } finally {
      setUploading(false);
    }
  }

  async function publish() {
    if ((!text.trim() && !imageUrl) || submitting) return;
    setSubmitting(true);
    try {
      await client.post('/posts', { text: text.trim(), imageUrl });
      setText('');
      setImageUrl(null);
      setOpen(false);
      onPosted();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="card">
      <div className="composer-top">
        <div className="avatar"></div>
        {!open ? (
          <div className="fake-input" onClick={() => setOpen(true)}>
            Exprimez-vous, qu'avez-vous en tête ?
          </div>
        ) : (
          <textarea
            className="composer-textarea"
            style={{ display: 'block' }}
            autoFocus
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="Exprimez-vous, qu'avez-vous en tête ?"
          />
        )}
      </div>
      {open && (
        <>
          {imageUrl && (
            <div style={{ position: 'relative', marginTop: 8 }}>
              <img src={imageUrl} alt="" style={{ width: '100%', borderRadius: 10, maxHeight: 260, objectFit: 'cover' }} />
              <button type="button" onClick={() => setImageUrl(null)}
                      style={{ position: 'absolute', top: 6, right: 6, border: 'none', borderRadius: '50%',
                               width: 26, height: 26, background: 'rgba(0,0,0,0.55)', color: '#fff', cursor: 'pointer' }}>✕</button>
            </div>
          )}
          {error && <p className="auth-error" style={{ marginTop: 6 }}>{error}</p>}
          <div className="composer-submit-row" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <label style={{ cursor: 'pointer', fontSize: 13, color: '#a08978' }}>
              📷 {uploading ? 'Envoi...' : 'Photo'}
              <input type="file" accept="image/*" onChange={handleFile} disabled={uploading}
                     style={{ display: 'none' }} />
            </label>
            <button
              className={`publish-btn ${(text.trim() || imageUrl) ? 'active' : ''}`}
              onClick={publish}
              disabled={submitting || uploading}
              style={{ marginLeft: 'auto' }}
            >
              Publier
            </button>
          </div>
        </>
      )}
    </div>
  );
}
