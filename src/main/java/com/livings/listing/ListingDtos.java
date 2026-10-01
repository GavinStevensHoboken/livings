package com.livings.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.livings.listing.ListingEnums.Amenity;
import com.livings.listing.ListingEnums.Category;
import com.livings.listing.ListingEnums.GenderPreference;
import com.livings.listing.ListingEnums.PropertyType;
import com.livings.listing.ListingEnums.RoomType;
import com.livings.listing.ListingEnums.Status;
import com.livings.university.UniversityResponse;
import com.livings.user.UserDtos.PublicUserResponse;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ListingDtos {

    private ListingDtos() {
    }

    public record ListingRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 5000) String description,
            @NotNull Category category,
            @NotNull RoomType roomType,
            @NotNull PropertyType propertyType,
            GenderPreference genderPreference,

            @NotBlank @Size(max = 255) String streetAddress,
            @Size(max = 50) String unit,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a two-letter state code") String state,
            @NotBlank String zipCode,
            Double latitude,
            Double longitude,
            Long nearestUniversityId,

            @NotNull @DecimalMin("1") @DecimalMax("50000") @Digits(integer = 8, fraction = 2) BigDecimal monthlyRent,
            @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal securityDeposit,
            boolean utilitiesIncluded,
            @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal estimatedUtilities,
            @NotNull LocalDate availableFrom,
            @Min(1) @Max(36) int minLeaseMonths,

            @Min(0) @Max(20) int bedrooms,
            @NotNull @DecimalMin("0") @DecimalMax("20") @Digits(integer = 2, fraction = 1) BigDecimal bathrooms,
            @Min(0) @Max(20) int currentOccupants,
            boolean furnished,
            boolean petsAllowed,

            boolean noSsnRequired,
            boolean noCreditHistoryRequired,
            boolean internationalGuarantorAccepted,
            boolean virtualTourAvailable,

            @Size(max = 20) List<@NotBlank @Size(max = 1000) String> photoUrls,
            Set<Amenity> amenities) {
    }

    public record StatusUpdateRequest(@NotNull Status status) {
    }

    public record ListingResponse(
            Long id,
            String title,
            String description,
            Category category,
            RoomType roomType,
            PropertyType propertyType,
            GenderPreference genderPreference,
            Status status,
            String streetAddress,
            String unit,
            String city,
            String state,
            String zipCode,
            Double latitude,
            Double longitude,
            UniversityResponse nearestUniversity,
            Double distanceToUniversityMiles,
            /** Distance from the search origin, present only on radius searches. */
            Double distanceMiles,
            BigDecimal monthlyRent,
            BigDecimal securityDeposit,
            boolean utilitiesIncluded,
            BigDecimal estimatedUtilities,
            LocalDate availableFrom,
            int minLeaseMonths,
            int bedrooms,
            BigDecimal bathrooms,
            int currentOccupants,
            boolean furnished,
            boolean petsAllowed,
            boolean noSsnRequired,
            boolean noCreditHistoryRequired,
            boolean internationalGuarantorAccepted,
            boolean virtualTourAvailable,
            List<String> photoUrls,
            Set<Amenity> amenities,
            PublicUserResponse owner,
            Instant createdAt,
            Instant updatedAt) {
    }
}
