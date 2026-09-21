package com.nexa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(length = 5000)
    private String text;

    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Post() {}

    public Post(User author, String text, String imageUrl) {
        this.author = author;
        this.text = text;
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
