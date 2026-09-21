package com.nexa.repository;

import com.nexa.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
        SELECT m FROM Message m
        WHERE (m.sender.id = :userA AND m.recipient.id = :userB)
           OR (m.sender.id = :userB AND m.recipient.id = :userA)
        ORDER BY m.createdAt ASC
        """)
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    long countByRecipientIdAndReadFalse(Long recipientId);
}
