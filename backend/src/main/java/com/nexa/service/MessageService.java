package com.nexa.service;

import com.nexa.dto.MessageDtos.*;
import com.nexa.model.Message;
import com.nexa.model.User;
import com.nexa.repository.MessageRepository;
import com.nexa.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public Message send(User sender, Long recipientId, String text) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new NoSuchElementException("Destinataire introuvable."));
        return messageRepository.save(new Message(sender, recipient, text));
    }

    public List<MessageDto> getConversation(Long userA, Long userB) {
        return messageRepository.findConversation(userA, userB).stream()
                .map(this::toDto)
                .toList();
    }

    /** Liste des conversations d'un utilisateur, triées par dernier message, avec compteur non-lus. */
    public List<ConversationPreview> getConversations(User me) {
        List<Message> all = messageRepository.findAll().stream()
                .filter(m -> m.getSender().getId().equals(me.getId()) || m.getRecipient().getId().equals(me.getId()))
                .sorted(Comparator.comparing(Message::getCreatedAt).reversed())
                .toList();

        Map<Long, ConversationPreview> byPartner = new LinkedHashMap<>();
        for (Message m : all) {
            User partner = m.getSender().getId().equals(me.getId()) ? m.getRecipient() : m.getSender();
            if (byPartner.containsKey(partner.getId())) continue;
            long unread = all.stream()
                    .filter(x -> x.getSender().getId().equals(partner.getId()) && !x.isRead())
                    .count();
            byPartner.put(partner.getId(), new ConversationPreview(
                    partner.getId(), partner.getFullName(), m.getText(), m.getCreatedAt(), unread));
        }
        return new ArrayList<>(byPartner.values());
    }

    public void markConversationRead(Long partnerId, Long meId) {
        messageRepository.findConversation(meId, partnerId).stream()
                .filter(m -> m.getRecipient().getId().equals(meId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
    }

    private MessageDto toDto(Message m) {
        return new MessageDto(m.getId(), m.getSender().getId(), m.getRecipient().getId(),
                m.getText(), m.getCreatedAt(), m.isRead());
    }
}
