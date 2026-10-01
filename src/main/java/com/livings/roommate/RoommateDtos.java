package com.livings.roommate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.livings.listing.ListingEnums.GenderPreference;
import com.livings.university.UniversityResponse;
import com.livings.user.Gender;
import com.livings.user.UserDtos.PublicUserResponse;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class RoommateDtos {

    private RoommateDtos() {
    }

    public record RoommateProfileRequest(
            @NotBlank @Size(max = 150) String headline,
            @NotNull @DecimalMin("0") @DecimalMax("50000") BigDecimal budgetMin,
            @NotNull @DecimalMin("1") @DecimalMax("50000") BigDecimal budgetMax,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a two-letter state code") String preferredState,
            @Size(max = 100) String preferredCity,
            Long universityId,
            @NotNull LocalDate moveInDate,
            @Min(1) @Max(36) Integer leaseMonths,
            GenderPreference genderPreference,
            boolean smoker,
            boolean hasPets,
            SleepSchedule sleepSchedule,
            @Min(1) @Max(5) Integer cleanlinessLevel,
            @Size(max = 2000) String bio,
            Boolean active) {
    }

    /** Query-string filters for {@code GET /api/v1/roommates}. */
    public record RoommateSearchCriteria(
            String state,
            String city,
            Long universityId,
            /** My budget: matches profiles whose range overlaps [minBudget, maxBudget]. */
            BigDecimal minBudget,
            BigDecimal maxBudget,
            LocalDate moveInFrom,
            LocalDate moveInTo,
            Gender gender,
            Boolean nonSmoker,
            Boolean noPets,
            SleepSchedule sleepSchedule) {
    }

    public record RoommateProfileResponse(
            Long id,
            PublicUserResponse user,
            Gender gender,
            String headline,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            String preferredState,
            String preferredCity,
            UniversityResponse university,
            LocalDate moveInDate,
            Integer leaseMonths,
            GenderPreference genderPreference,
            boolean smoker,
            boolean hasPets,
            SleepSchedule sleepSchedule,
            Integer cleanlinessLevel,
            String bio,
            boolean active,
            Instant updatedAt) {

        public static RoommateProfileResponse from(RoommateProfile p) {
            return new RoommateProfileResponse(p.getId(), PublicUserResponse.from(p.getUser()),
                    p.getUser().getGender(), p.getHeadline(), p.getBudgetMin(), p.getBudgetMax(),
                    p.getPreferredState().name(), p.getPreferredCity(),
                    p.getUniversity() == null ? null : UniversityResponse.from(p.getUniversity()),
                    p.getMoveInDate(), p.getLeaseMonths(), p.getGenderPreference(), p.isSmoker(), p.isHasPets(),
                    p.getSleepSchedule(), p.getCleanlinessLevel(), p.getBio(), p.isActive(), p.getUpdatedAt());
        }
    }
}
