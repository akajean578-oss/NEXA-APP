package com.nexa.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class MessageDtos {

    public record SendMessageRequest(@NotBlank Long recipientId, @NotBlank String text) {}

    public record MessageDto(
            Long id,
            Long senderId,
            Long recipientId,
            String text,
            Instant createdAt,
            boolean read
    ) {}

    public record ConversationPreview(
            Long userId,
            String userFullName,
            String lastMessage,
            Instant lastMessageAt,
            long unreadCount
    ) {}
}
