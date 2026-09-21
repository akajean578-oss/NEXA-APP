package com.nexa.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class PostDtos {

    public record CreatePostRequest(@NotBlank String text, String imageUrl) {}

    public record CommentRequest(@NotBlank String text) {}

    public record AuthorDto(Long id, String fullName, String avatarUrl) {}

    public record CommentDto(Long id, AuthorDto author, String text, Instant createdAt) {}

    public record PostDto(
            Long id,
            AuthorDto author,
            String text,
            String imageUrl,
            Instant createdAt,
            long likeCount,
            boolean likedByMe,
            long commentCount
    ) {}
}
