package com.nexa.model;

import jakarta.persistence.*;

@Entity
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    public Like() {}

    public Like(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public User getUser() { return user; }
}
