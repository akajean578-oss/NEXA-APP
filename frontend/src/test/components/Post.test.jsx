import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Post from '../../components/Post.jsx';
import client from '../../api/client';
import * as realtime from '../../realtime';

vi.mock('../../api/client');
vi.mock('../../realtime', () => ({
  connectRealtime: vi.fn(() => null),
}));

const basePost = {
  id: 1,
  author: { fullName: 'Alice Test' },
  text: 'Un post de démonstration',
  imageUrl: null,
  createdAt: new Date().toISOString(),
  likeCount: 3,
  likedByMe: false,
  commentCount: 2,
};

describe('Post', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche le texte, l\'auteur et les compteurs', () => {
    render(<Post post={basePost} />);
    expect(screen.getByText('Un post de démonstration')).toBeInTheDocument();
    expect(screen.getByText('Alice Test')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2 commentaires')).toBeInTheDocument();
  });

  it('bascule le like au clic et appelle l\'API', async () => {
    client.post.mockResolvedValueOnce({ data: { liked: true } });
    render(<Post post={basePost} />);

    fireEvent.click(screen.getByText("J'aime"));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/posts/1/like');
    });
  });

  it('charge et affiche les commentaires au clic sur "Commenter"', async () => {
    client.get.mockResolvedValueOnce({
      data: [{ id: 10, author: { fullName: 'Bob' }, text: 'Bravo !' }],
    });

    render(<Post post={basePost} />);
    fireEvent.click(screen.getByText('Commenter'));

    await waitFor(() => {
      expect(screen.getByText('Bravo !')).toBeInTheDocument();
    });
    expect(client.get).toHaveBeenCalledWith('/posts/1/comments');
  });
});
