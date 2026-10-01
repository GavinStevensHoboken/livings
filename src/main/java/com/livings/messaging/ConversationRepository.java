package com.livings.messaging;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByListingIdAndInitiatorId(Long listingId, Long initiatorId);

    @Query("""
            select c from Conversation c
            where c.listing is null
              and ((c.initiator.id = :a and c.recipient.id = :b) or (c.initiator.id = :b and c.recipient.id = :a))
            """)
    Optional<Conversation> findDirect(Long a, Long b);

    @Query("select c from Conversation c where c.initiator.id = :userId or c.recipient.id = :userId")
    Page<Conversation> findForUser(Long userId, Pageable pageable);
}
