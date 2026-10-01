package com.livings.listing;

import java.util.Set;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.livings.common.PageResponse;
import com.livings.common.Pageables;
import com.livings.listing.ListingDtos.ListingRequest;
import com.livings.listing.ListingDtos.ListingResponse;
import com.livings.listing.ListingDtos.StatusUpdateRequest;
import com.livings.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/listings")
@Tag(name = "Listings")
public class ListingController {

    private static final Set<String> SORTABLE = Set.of("createdAt", "monthlyRent", "availableFrom", "bedrooms");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ListingService service;

    public ListingController(ListingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search active listings",
            description = "Public. Combine filters freely. Radius search: universityId or latitude+longitude "
                    + "with radiusMiles (default 10, max 100). Sort by createdAt, monthlyRent, availableFrom, bedrooms.")
    public PageResponse<ListingResponse> search(@ParameterObject ListingSearchCriteria criteria,
                                                @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.search(criteria, Pageables.sanitize(pageable, SORTABLE, DEFAULT_SORT));
    }

    @GetMapping("/{id}")
    public ListingResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser viewer) {
        return service.get(id, viewer);
    }

    @GetMapping("/mine")
    @Operation(summary = "Listings I posted (excluding archived)")
    public PageResponse<ListingResponse> mine(@AuthenticationPrincipal AuthUser me,
                                              @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.mine(me, Pageables.sanitize(pageable, SORTABLE, DEFAULT_SORT));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a private rental or roommate listing (contiguous US + DC only)")
    public ListingResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ListingRequest request) {
        return service.create(me, request);
    }

    @PutMapping("/{id}")
    public ListingResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody ListingRequest request) {
        return service.update(me, id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Pause, re-activate or mark a listing as rented")
    public ListingResponse updateStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(me, id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive a listing (soft delete)")
    public void archive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.archive(me, id);
    }
}
