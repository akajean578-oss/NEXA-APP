package com.nexa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(nullable = false, length = 3000)
    private String text;

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Message() {}

    public Message(User sender, User recipient, String text) {
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
    }

    public Long getId() { return id; }
    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public String getText() { return text; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
}
