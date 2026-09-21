package com.nexa.repository;

import com.nexa.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
