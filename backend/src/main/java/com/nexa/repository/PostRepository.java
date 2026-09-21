package com.nexa.repository;

import com.nexa.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByTextContainingIgnoreCaseOrderByCreatedAtDesc(String text);
}
