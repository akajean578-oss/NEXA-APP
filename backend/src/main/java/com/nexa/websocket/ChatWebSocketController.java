package com.nexa.websocket;

import com.nexa.dto.MessageDtos.MessageDto;
import com.nexa.dto.MessageDtos.SendMessageRequest;
import com.nexa.model.Message;
import com.nexa.model.User;
import com.nexa.repository.UserRepository;
import com.nexa.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService, UserRepository userRepository,
                                    SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Le client envoie sur /app/chat.send. On persiste le message en base (donc
     * on garde l'historique) ET on le pousse instantanément à l'expéditeur et au
     * destinataire via leur file privée /user/queue/messages.
     */
    @MessageMapping("/chat.send")
    public void send(SendMessageRequest req, Authentication authentication) {
        String senderEmail = (String) authentication.getPrincipal();
        User sender = userRepository.findByEmail(senderEmail).orElseThrow();

        Message saved = messageService.send(sender, req.recipientId(), req.text());
        MessageDto dto = new MessageDto(saved.getId(), sender.getId(), saved.getRecipient().getId(),
                saved.getText(), saved.getCreatedAt(), saved.isRead());

        // Poussé au destinataire (notification live du chat)
        messagingTemplate.convertAndSendToUser(saved.getRecipient().getEmail(), "/queue/messages", dto);
        // Renvoyé aussi à l'expéditeur (pour confirmer/synchroniser ses autres appareils/onglets)
        messagingTemplate.convertAndSendToUser(senderEmail, "/queue/messages", dto);
    }

    /** Indicateur "X est en train d'écrire..." */
    @MessageMapping("/chat.typing")
    public void typing(SendMessageRequest req, Authentication authentication) {
        String senderEmail = (String) authentication.getPrincipal();
        User sender = userRepository.findByEmail(senderEmail).orElseThrow();
        User recipient = userRepository.findById(req.recipientId()).orElseThrow();

        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/typing",
                java.util.Map.of("fromUserId", sender.getId(), "fromName", sender.getFullName()));
    }
}
