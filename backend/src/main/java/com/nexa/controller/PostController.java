package com.nexa.controller;

import com.nexa.dto.PostDtos.*;
import com.nexa.model.Comment;
import com.nexa.model.Post;
import com.nexa.model.User;
import com.nexa.repository.UserRepository;
import com.nexa.service.PostService;
import com.nexa.websocket.RealtimeNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;
    private final RealtimeNotificationService realtime;

    public PostController(PostService postService, UserRepository userRepository,
                           RealtimeNotificationService realtime) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.realtime = realtime;
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }

    @GetMapping
    public List<PostDto> feed(@AuthenticationPrincipal UserDetails principal) {
        User user = principal != null ? currentUser(principal) : null;
        return postService.getFeed(user);
    }

    @PostMapping
    public ResponseEntity<Post> create(@AuthenticationPrincipal UserDetails principal,
                                        @Valid @RequestBody CreatePostRequest req) {
        User author = currentUser(principal);
        Post post = postService.createPost(author, req.text(), req.imageUrl());
        realtime.broadcastNewPost(postService.toDtoPublic(post, author));
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Boolean>> like(@AuthenticationPrincipal UserDetails principal,
                                                       @PathVariable Long id) {
        User user = currentUser(principal);
        boolean liked = postService.toggleLike(id, user);
        long count = postService.getLikeCount(id);
        realtime.broadcastLikeUpdate(id, count);

        String authorEmail = postService.getAuthorEmail(id);
        if (liked && authorEmail != null && !authorEmail.equals(user.getEmail())) {
            realtime.notifyUser(authorEmail, "like", user.getFullName() + " a aimé votre publication.");
        }
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @GetMapping("/{id}/comments")
    public List<CommentDto> comments(@PathVariable Long id) {
        return postService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable Long id,
                                               @Valid @RequestBody CommentRequest req) {
        User author = currentUser(principal);
        Comment comment = postService.addComment(id, author, req.text());
        CommentDto dto = new CommentDto(comment.getId(),
                new AuthorDto(author.getId(), author.getFullName(), author.getAvatarUrl()),
                comment.getText(), comment.getCreatedAt());
        realtime.broadcastNewComment(id, dto);

        String authorEmail = postService.getAuthorEmail(id);
        if (authorEmail != null && !authorEmail.equals(author.getEmail())) {
            realtime.notifyUser(authorEmail, "comment", author.getFullName() + " a commenté votre publication.");
        }
        return ResponseEntity.ok(comment);
    }
}
