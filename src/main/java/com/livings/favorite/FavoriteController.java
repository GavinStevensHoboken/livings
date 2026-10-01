package com.livings.favorite;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.livings.common.PageResponse;
import com.livings.listing.ListingDtos.ListingResponse;
import com.livings.listing.ListingService;
import com.livings.security.AuthUser;
import com.livings.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "Favorites")
public class FavoriteController {

    private final FavoriteRepository favorites;
    private final ListingService listingService;
    private final UserService userService;

    public FavoriteController(FavoriteRepository favorites, ListingService listingService, UserService userService) {
        this.favorites = favorites;
        this.listingService = listingService;
        this.userService = userService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "My saved listings, most recently saved first")
    public PageResponse<ListingResponse> list(@AuthenticationPrincipal AuthUser me,
                                              @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Pageable byNewest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(favorites.findVisibleByUserId(me.id(), byNewest),
                f -> listingService.toResponse(f.getListing()));
    }

    @PutMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(summary = "Save a listing (idempotent)")
    public void add(@AuthenticationPrincipal AuthUser me, @PathVariable Long listingId) {
        if (favorites.existsByUserIdAndListingId(me.id(), listingId)) {
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUser(userService.require(me.id()));
        favorite.setListing(listingService.requireVisible(listingId));
        favorites.save(favorite);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@AuthenticationPrincipal AuthUser me, @PathVariable Long listingId) {
        favorites.deleteByUserIdAndListingId(me.id(), listingId);
    }
}
