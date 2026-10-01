package com.livings.listing;

import java.util.List;
import java.util.Set;

import com.livings.geo.GeoUtils;
import com.livings.listing.ListingDtos.ListingResponse;
import com.livings.university.University;
import com.livings.university.UniversityResponse;
import com.livings.user.UserDtos.PublicUserResponse;

final class ListingMapper {

    private ListingMapper() {
    }

    static ListingResponse toResponse(Listing l, ListingSpecifications.GeoOrigin origin) {
        University uni = l.getNearestUniversity();
        boolean hasCoords = l.getLatitude() != null && l.getLongitude() != null;
        Double toUniversity = uni != null && hasCoords
                ? round(GeoUtils.distanceMiles(l.getLatitude(), l.getLongitude(), uni.getLatitude(), uni.getLongitude()))
                : null;
        Double fromOrigin = origin != null && hasCoords
                ? round(GeoUtils.distanceMiles(origin.latitude(), origin.longitude(), l.getLatitude(), l.getLongitude()))
                : null;
        return new ListingResponse(
                l.getId(), l.getTitle(), l.getDescription(), l.getCategory(), l.getRoomType(), l.getPropertyType(),
                l.getGenderPreference(), l.getStatus(), l.getStreetAddress(), l.getUnit(), l.getCity(),
                l.getState().name(), l.getZipCode(), l.getLatitude(), l.getLongitude(),
                uni == null ? null : UniversityResponse.from(uni), toUniversity, fromOrigin,
                l.getMonthlyRent(), l.getSecurityDeposit(), l.isUtilitiesIncluded(), l.getEstimatedUtilities(),
                l.getAvailableFrom(), l.getMinLeaseMonths(), l.getBedrooms(), l.getBathrooms(),
                l.getCurrentOccupants(), l.isFurnished(), l.isPetsAllowed(), l.isNoSsnRequired(),
                l.isNoCreditHistoryRequired(), l.isInternationalGuarantorAccepted(), l.isVirtualTourAvailable(),
                List.copyOf(l.getPhotoUrls()), Set.copyOf(l.getAmenities()),
                PublicUserResponse.from(l.getOwner()), l.getCreatedAt(), l.getUpdatedAt());
    }

    private static double round(double miles) {
        return Math.round(miles * 10.0) / 10.0;
    }
}
