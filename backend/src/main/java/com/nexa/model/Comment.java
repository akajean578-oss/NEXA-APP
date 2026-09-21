package com.nexa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Comment() {}

    public Comment(Post post, User author, String text) {
        this.post = post;
        this.author = author;
        this.text = text;
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public User getAuthor() { return author; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}
