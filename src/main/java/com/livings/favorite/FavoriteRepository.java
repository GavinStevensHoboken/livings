package com.livings.favorite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    @Modifying
    @Query("delete from Favorite f where f.user.id = :userId and f.listing.id = :listingId")
    int deleteByUserIdAndListingId(Long userId, Long listingId);

    @Query("select f from Favorite f where f.user.id = :userId and f.listing.status <> com.livings.listing.ListingEnums.Status.ARCHIVED")
    Page<Favorite> findVisibleByUserId(Long userId, Pageable pageable);
}
