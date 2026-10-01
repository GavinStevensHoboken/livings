package com.livings.messaging;

import java.time.Instant;

import com.livings.user.UserDtos.PublicUserResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MessagingDtos {

    private MessagingDtos() {
    }

    /** Start (or continue) a thread: set {@code listingId} to ask about a listing, or {@code recipientId} for a direct chat. */
    public record StartConversationRequest(
            Long listingId,
            Long recipientId,
            @NotBlank @Size(max = 4000) String message) {
    }

    public record SendMessageRequest(@NotBlank @Size(max = 4000) String body) {
    }

    public record ListingSummary(Long id, String title, String city, String state, String coverPhotoUrl) {
    }

    public record ConversationResponse(
            Long id,
            ListingSummary listing,
            PublicUserResponse otherParticipant,
            MessageResponse lastMessage,
            long unreadCount,
            Instant lastMessageAt) {
    }

    public record MessageResponse(
            Long id,
            Long conversationId,
            Long senderId,
            String body,
            Instant sentAt,
            Instant readAt) {

        static MessageResponse from(Message m) {
            return new MessageResponse(m.getId(), m.getConversation().getId(), m.getSender().getId(), m.getBody(),
                    m.getSentAt(), m.getReadAt());
        }
    }
}
