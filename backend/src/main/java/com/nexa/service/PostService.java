package com.nexa.service;

import com.nexa.dto.PostDtos.*;
import com.nexa.model.*;
import com.nexa.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                        LikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }

    /**
     * La liste brute des posts (avant calcul des likes/commentaires par utilisateur)
     * est mise en cache : c'est la requête la plus fréquente de toute l'application
     * (chargée à chaque ouverture du fil), donc celle qui profite le plus d'éviter
     * un aller-retour base de données à chaque appel.
     */
    @Cacheable("feedPosts")
    public List<Post> getRawFeed() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<PostDto> getFeed(User currentUser) {
        return getRawFeed().stream()
                .map(post -> toDto(post, currentUser))
                .toList();
    }

    @CacheEvict(value = "feedPosts", allEntries = true)
    public Post createPost(User author, String text, String imageUrl) {
        return postRepository.save(new Post(author, text, imageUrl));
    }

    public boolean toggleLike(Long postId, User user) {
        var existing = likeRepository.findByPostIdAndUserId(postId, user.getId());
        if (existing.isPresent()) {
            likeRepository.deleteByPostIdAndUserId(postId, user.getId());
            return false; // plus liké
        } else {
            Post post = postRepository.findById(postId).orElseThrow();
            likeRepository.save(new Like(post, user));
            return true; // maintenant liké
        }
    }

    public Comment addComment(Long postId, User author, String text) {
        Post post = postRepository.findById(postId).orElseThrow();
        return commentRepository.save(new Comment(post, author, text));
    }

    public List<CommentDto> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(c -> new CommentDto(
                        c.getId(),
                        new AuthorDto(c.getAuthor().getId(), c.getAuthor().getFullName(), c.getAuthor().getAvatarUrl()),
                        c.getText(),
                        c.getCreatedAt()))
                .toList();
    }

    public long getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    public String getAuthorEmail(Long postId) {
        return postRepository.findById(postId).map(p -> p.getAuthor().getEmail()).orElse(null);
    }

    public PostDto toDtoPublic(Post post, User currentUser) {
        return toDto(post, currentUser);
    }

    private PostDto toDto(Post post, User currentUser) {
        long likeCount = likeRepository.countByPostId(post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());
        boolean likedByMe = currentUser != null &&
                likeRepository.findByPostIdAndUserId(post.getId(), currentUser.getId()).isPresent();

        return new PostDto(
                post.getId(),
                new AuthorDto(post.getAuthor().getId(), post.getAuthor().getFullName(), post.getAuthor().getAvatarUrl()),
                post.getText(),
                post.getImageUrl(),
                post.getCreatedAt(),
                likeCount,
                likedByMe,
                commentCount
        );
    }
}
