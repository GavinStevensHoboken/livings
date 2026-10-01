package com.livings.messaging;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livings.common.ApiException;
import com.livings.common.PageResponse;
import com.livings.listing.Listing;
import com.livings.listing.ListingService;
import com.livings.messaging.MessagingDtos.ConversationResponse;
import com.livings.messaging.MessagingDtos.ListingSummary;
import com.livings.messaging.MessagingDtos.MessageResponse;
import com.livings.messaging.MessagingDtos.StartConversationRequest;
import com.livings.security.AuthUser;
import com.livings.user.User;
import com.livings.user.UserDtos.PublicUserResponse;
import com.livings.user.UserService;

@Service
public class MessagingService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ListingService listingService;
    private final UserService userService;

    public MessagingService(ConversationRepository conversations, MessageRepository messages,
                            ListingService listingService, UserService userService) {
        this.conversations = conversations;
        this.messages = messages;
        this.listingService = listingService;
        this.userService = userService;
    }

    @Transactional
    public ConversationResponse start(AuthUser me, StartConversationRequest request) {
        if ((request.listingId() == null) == (request.recipientId() == null)) {
            throw ApiException.badRequest("Provide exactly one of listingId or recipientId");
        }
        User sender = userService.require(me.id());
        Conversation conversation;
        if (request.listingId() != null) {
            Listing listing = listingService.requireVisible(request.listingId());
            if (listing.getOwner().getId().equals(me.id())) {
                throw ApiException.badRequest("You cannot message your own listing");
            }
            conversation = conversations.findByListingIdAndInitiatorId(listing.getId(), me.id())
                    .orElseGet(() -> newConversation(listing, sender, listing.getOwner()));
        } else {
            if (request.recipientId().equals(me.id())) {
                throw ApiException.badRequest("You cannot message yourself");
            }
            User recipient = userService.require(request.recipientId());
            conversation = conversations.findDirect(me.id(), recipient.getId())
                    .orElseGet(() -> newConversation(null, sender, recipient));
        }
        append(conversation, sender, request.message());
        return toResponse(conversation, me.id());
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> list(AuthUser me, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        return PageResponse.of(conversations.findForUser(me.id(), newestFirst), c -> toResponse(c, me.id()));
    }

    /** Newest messages first; opening a page marks the other person's messages as read. */
    @Transactional
    public PageResponse<MessageResponse> messages(AuthUser me, Long conversationId, Pageable pageable) {
        Conversation conversation = requireParticipant(me, conversationId);
        messages.markRead(conversation.getId(), me.id(), Instant.now());
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "sentAt"));
        return PageResponse.of(messages.findByConversationId(conversation.getId(), newestFirst), MessageResponse::from);
    }

    @Transactional
    public MessageResponse send(AuthUser me, Long conversationId, String body) {
        Conversation conversation = requireParticipant(me, conversationId);
        return MessageResponse.from(append(conversation, userService.require(me.id()), body));
    }

    private Conversation newConversation(Listing listing, User initiator, User recipient) {
        Conversation c = new Conversation();
        c.setListing(listing);
        c.setInitiator(initiator);
        c.setRecipient(recipient);
        c.setCreatedAt(Instant.now());
        c.setLastMessageAt(c.getCreatedAt());
        return conversations.save(c);
    }

    private Message append(Conversation conversation, User sender, String body) {
        Message m = new Message();
        m.setConversation(conversation);
        m.setSender(sender);
        m.setBody(body.trim());
        m.setSentAt(Instant.now());
        conversation.setLastMessageAt(m.getSentAt());
        // TODO: fan out an APNs push to the other participant here.
        return messages.save(m);
    }

    private Conversation requireParticipant(AuthUser me, Long conversationId) {
        Conversation c = conversations.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation", conversationId));
        if (!c.hasParticipant(me.id())) {
            // 404 rather than 403 so conversation ids cannot be probed.
            throw ApiException.notFound("Conversation", conversationId);
        }
        return c;
    }

    private ConversationResponse toResponse(Conversation c, Long viewerId) {
        Listing l = c.getListing();
        ListingSummary listing = l == null ? null : new ListingSummary(l.getId(), l.getTitle(), l.getCity(),
                l.getState().name(), l.getPhotoUrls().isEmpty() ? null : l.getPhotoUrls().getFirst());
        MessageResponse last = messages.findFirstByConversationIdOrderBySentAtDesc(c.getId())
                .map(MessageResponse::from).orElse(null);
        return new ConversationResponse(c.getId(), listing, PublicUserResponse.from(c.otherParticipant(viewerId)),
                last, messages.countUnread(c.getId(), viewerId), c.getLastMessageAt());
    }
}
