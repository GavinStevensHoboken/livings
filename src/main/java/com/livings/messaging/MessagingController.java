package com.livings.messaging;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.livings.common.PageResponse;
import com.livings.messaging.MessagingDtos.ConversationResponse;
import com.livings.messaging.MessagingDtos.MessageResponse;
import com.livings.messaging.MessagingDtos.SendMessageRequest;
import com.livings.messaging.MessagingDtos.StartConversationRequest;
import com.livings.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Messaging")
public class MessagingController {

    private final MessagingService service;

    public MessagingController(MessagingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Message a lister about a listing, or a student found via roommate search",
            description = "Reuses the existing thread if there is one.")
    public ConversationResponse start(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody StartConversationRequest request) {
        return service.start(me, request);
    }

    @GetMapping
    @Operation(summary = "My inbox, most recent activity first")
    public PageResponse<ConversationResponse> list(@AuthenticationPrincipal AuthUser me,
                                                   @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.list(me, pageable);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Messages in a thread, newest first; marks incoming messages as read")
    public PageResponse<MessageResponse> messages(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                  @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return service.messages(me, id, pageable);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                @Valid @RequestBody SendMessageRequest request) {
        return service.send(me, id, request.body());
    }
}
