package com.nexa.repository;

import com.nexa.model.Friendship;
import com.nexa.model.Friendship.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :a AND f.addressee.id = :b)
           OR (f.requester.id = :b AND f.addressee.id = :a)
        """)
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    List<Friendship> findByAddresseeIdAndStatus(Long addresseeId, Status status);

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId) AND f.status = 'ACCEPTED'
        """)
    List<Friendship> findAcceptedFor(@Param("userId") Long userId);
}
