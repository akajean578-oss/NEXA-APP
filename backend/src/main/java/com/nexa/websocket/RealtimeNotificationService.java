package com.nexa.websocket;

import com.nexa.dto.PostDtos.PostDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Diffuse un nouveau post à tout le monde connecté au fil d'actualité. */
    public void broadcastNewPost(PostDto post) {
        messagingTemplate.convertAndSend("/topic/feed", post);
    }

    /** Diffuse la mise à jour du compteur de likes d'un post à tout le monde. */
    public void broadcastLikeUpdate(Long postId, long likeCount) {
        messagingTemplate.convertAndSend("/topic/posts/" + postId + "/likes",
                Map.of("postId", postId, "likeCount", likeCount));
    }

    /** Diffuse un nouveau commentaire à tout le monde regardant ce post. */
    public void broadcastNewComment(Long postId, Object comment) {
        messagingTemplate.convertAndSend("/topic/posts/" + postId + "/comments", comment);
    }

    /** Notification privée envoyée uniquement à un utilisateur précis (ex : "X a aimé votre post"). */
    public void notifyUser(String recipientEmail, String type, String message) {
        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/notifications",
                Map.of("type", type, "message", message, "at", Instant.now().toString()));
    }
}
