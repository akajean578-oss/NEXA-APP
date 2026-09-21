import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Composer from '../../components/Composer.jsx';
import client from '../../api/client';

vi.mock('../../api/client');

describe('Composer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("affiche un champ replié qui s'ouvre au clic", async () => {
    render(<Composer onPosted={vi.fn()} />);

    expect(screen.getByText(/exprimez-vous/i)).toBeInTheDocument();
    await userEvent.click(screen.getByText(/exprimez-vous/i));

    expect(screen.getByPlaceholderText(/exprimez-vous/i)).toBeInTheDocument();
  });

  it('le bouton Publier est désactivé tant que le texte est vide', async () => {
    render(<Composer onPosted={vi.fn()} />);
    await userEvent.click(screen.getByText(/exprimez-vous/i));

    const button = screen.getByRole('button', { name: /publier/i });
    expect(button).not.toHaveClass('active');
  });

  it('publie le post et appelle onPosted au succès', async () => {
    client.post.mockResolvedValueOnce({ data: { id: 1 } });
    const onPosted = vi.fn();

    render(<Composer onPosted={onPosted} />);
    await userEvent.click(screen.getByText(/exprimez-vous/i));

    const textarea = screen.getByPlaceholderText(/exprimez-vous/i);
    await userEvent.type(textarea, 'Mon nouveau post');

    const button = screen.getByRole('button', { name: /publier/i });
    fireEvent.click(button);

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/posts', {
        text: 'Mon nouveau post', imageUrl: null,
      });
      expect(onPosted).toHaveBeenCalled();
    });
  });
});
