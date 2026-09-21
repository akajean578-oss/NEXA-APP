package com.nexa.service;

import com.nexa.dto.PostDtos.PostDto;
import com.nexa.model.Comment;
import com.nexa.model.Like;
import com.nexa.model.Post;
import com.nexa.model.User;
import com.nexa.repository.CommentRepository;
import com.nexa.repository.LikeRepository;
import com.nexa.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste la logique métier de PostService en isolation (les repositories sont
 * simulés avec Mockito) : pas besoin d'une vraie base de données pour ces tests.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private LikeRepository likeRepository;

    @InjectMocks private PostService postService;

    private User author;
    private User viewer;
    private Post post;

    @BeforeEach
    void setUp() throws Exception {
        author = new User("author@nexa.dev", "hashed", "Auteur Test");
        setId(author, 1L);
        viewer = new User("viewer@nexa.dev", "hashed", "Lecteur Test");
        setId(viewer, 2L);
        post = new Post(author, "Mon premier post", null);
        setId(post, 100L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }

    @Test
    void createPost_sauvegardeLePostViaLeRepository() {
        when(postRepository.save(any(Post.class))).thenReturn(post);

        Post result = postService.createPost(author, "Mon premier post", null);

        assertThat(result.getText()).isEqualTo("Mon premier post");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void toggleLike_ajouteUnLikeSiAucunNExiste() {
        when(likeRepository.findByPostIdAndUserId(100L, 2L)).thenReturn(Optional.empty());
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        boolean liked = postService.toggleLike(100L, viewer);

        assertThat(liked).isTrue();
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void toggleLike_retireLeLikeSiDejaPresent() {
        when(likeRepository.findByPostIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(new Like(post, viewer)));

        boolean liked = postService.toggleLike(100L, viewer);

        assertThat(liked).isFalse();
        verify(likeRepository).deleteByPostIdAndUserId(100L, 2L);
    }

    @Test
    void getFeed_retourneLesPostsAvecLeursCompteurs() {
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(post));
        when(likeRepository.countByPostId(100L)).thenReturn(3L);
        when(commentRepository.countByPostId(100L)).thenReturn(2L);
        when(likeRepository.findByPostIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        List<PostDto> feed = postService.getFeed(viewer);

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).likeCount()).isEqualTo(3L);
        assertThat(feed.get(0).commentCount()).isEqualTo(2L);
        assertThat(feed.get(0).likedByMe()).isFalse();
    }

    @Test
    void addComment_persisteLeCommentaireLieAuPost() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        Comment saved = new Comment(post, viewer, "Bravo !");
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        Comment result = postService.addComment(100L, viewer, "Bravo !");

        assertThat(result.getText()).isEqualTo("Bravo !");
        verify(commentRepository).save(any(Comment.class));
    }
}
