package com.livings.listing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import com.livings.listing.ListingEnums.Amenity;
import com.livings.listing.ListingEnums.Category;
import com.livings.listing.ListingEnums.GenderPreference;
import com.livings.listing.ListingEnums.PropertyType;
import com.livings.listing.ListingEnums.RoomType;

/**
 * Query-string filters for {@code GET /api/v1/listings}. All fields are optional.
 * Radius search: pass {@code universityId} or {@code latitude}+{@code longitude}, plus {@code radiusMiles}.
 */
public record ListingSearchCriteria(
        String q,
        String state,
        String city,
        String zipCode,
        Category category,
        RoomType roomType,
        PropertyType propertyType,
        GenderPreference genderPreference,
        BigDecimal minRent,
        BigDecimal maxRent,
        Integer minBedrooms,
        BigDecimal minBathrooms,
        /** Only listings available on or before this date. */
        LocalDate availableBy,
        /** Only listings whose minimum lease is at most this many months (short-stay / one-semester). */
        Integer maxLeaseMonths,
        Boolean furnished,
        Boolean petsAllowed,
        Boolean utilitiesIncluded,
        Boolean noSsnRequired,
        Boolean noCreditHistoryRequired,
        Boolean internationalGuarantorAccepted,
        Boolean virtualTourAvailable,
        Set<Amenity> amenities,
        Long universityId,
        Double latitude,
        Double longitude,
        Double radiusMiles) {
}
