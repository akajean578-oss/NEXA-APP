package com.nexa.controller;

import com.nexa.model.Post;
import com.nexa.model.User;
import com.nexa.repository.PostRepository;
import com.nexa.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public SearchController(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public record UserResult(Long id, String fullName, String avatarUrl) {}
    public record PostResult(Long id, String authorName, String text, String createdAt) {}

    @GetMapping
    public Map<String, Object> search(@RequestParam("q") String query) {
        if (query == null || query.isBlank() || query.length() > 100) {
            return Map.of("users", List.of(), "posts", List.of());
        }
        String q = query.trim();

        List<UserResult> users = userRepository.findByFullNameContainingIgnoreCase(q).stream()
                .limit(10)
                .map(u -> new UserResult(u.getId(), u.getFullName(), u.getAvatarUrl()))
                .toList();

        List<PostResult> posts = postRepository.findByTextContainingIgnoreCaseOrderByCreatedAtDesc(q).stream()
                .limit(20)
                .map(p -> new PostResult(p.getId(), p.getAuthor().getFullName(), p.getText(), p.getCreatedAt().toString()))
                .toList();

        return Map.of("users", users, "posts", posts);
    }
}
