package com.livings.roommate;

import java.util.Set;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.livings.common.PageResponse;
import com.livings.common.Pageables;
import com.livings.roommate.RoommateDtos.RoommateProfileRequest;
import com.livings.roommate.RoommateDtos.RoommateProfileResponse;
import com.livings.roommate.RoommateDtos.RoommateSearchCriteria;
import com.livings.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/roommates")
@Tag(name = "Roommate matching")
public class RoommateController {

    private static final Set<String> SORTABLE = Set.of("updatedAt", "moveInDate", "budgetMax", "budgetMin");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "updatedAt");

    private final RoommateService service;

    public RoommateController(RoommateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Find students looking for roommates",
            description = "Signed-in users only. Respects each profile's gender preference toward the viewer.")
    public PageResponse<RoommateProfileResponse> search(@AuthenticationPrincipal AuthUser me,
                                                        @ParameterObject RoommateSearchCriteria criteria,
                                                        @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.search(me, criteria, Pageables.sanitize(pageable, SORTABLE, DEFAULT_SORT));
    }

    @GetMapping("/{id}")
    public RoommateProfileResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/me")
    public RoommateProfileResponse mine(@AuthenticationPrincipal AuthUser me) {
        return service.mine(me);
    }

    @PutMapping("/me")
    @Operation(summary = "Create or replace my roommate profile")
    public RoommateProfileResponse upsert(@AuthenticationPrincipal AuthUser me,
                                          @Valid @RequestBody RoommateProfileRequest request) {
        return service.upsert(me, request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hide my roommate profile from search")
    public void deactivate(@AuthenticationPrincipal AuthUser me) {
        service.deactivate(me);
    }
}
