package com.nexa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "addressee_id"}))
public class Friendship {

    public enum Status { PENDING, ACCEPTED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "addressee_id")
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Friendship() {}

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
    }

    public Long getId() { return id; }
    public User getRequester() { return requester; }
    public User getAddressee() { return addressee; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
