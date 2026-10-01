package com.livings.messaging;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationId(Long conversationId, Pageable pageable);

    Optional<Message> findFirstByConversationIdOrderBySentAtDesc(Long conversationId);

    @Query("""
            select count(m) from Message m
            where m.conversation.id = :conversationId and m.sender.id <> :userId and m.readAt is null
            """)
    long countUnread(Long conversationId, Long userId);

    @Modifying
    @Query("""
            update Message m set m.readAt = :now
            where m.conversation.id = :conversationId and m.sender.id <> :userId and m.readAt is null
            """)
    int markRead(Long conversationId, Long userId, Instant now);
}
