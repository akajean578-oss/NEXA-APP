package com.nexa.controller;

import com.nexa.dto.MessageDtos.*;
import com.nexa.model.User;
import com.nexa.repository.UserRepository;
import com.nexa.service.MessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    public MessageController(MessageService messageService, UserRepository userRepository) {
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }

    /** Liste des conversations (aperçu) de l'utilisateur connecté. */
    @GetMapping
    public List<ConversationPreview> conversations(@AuthenticationPrincipal UserDetails principal) {
        return messageService.getConversations(currentUser(principal));
    }

    /** Historique complet d'une conversation avec un utilisateur donné, et marque les messages comme lus. */
    @GetMapping("/{partnerId}")
    public List<MessageDto> conversation(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long partnerId) {
        User me = currentUser(principal);
        List<MessageDto> history = messageService.getConversation(me.getId(), partnerId);
        messageService.markConversationRead(partnerId, me.getId());
        return history;
    }
}
