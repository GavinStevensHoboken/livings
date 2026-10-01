package com.livings.user;

import java.time.Instant;

import com.livings.university.University;
import com.livings.university.UniversityResponse;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull Role role,
            @Size(max = 80) String homeCountry,
            Long universityId) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record AuthResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            UserResponse user) {
    }

    public record UpdateProfileRequest(
            @Size(max = 120) String fullName,
            @Size(max = 30) String phone,
            Gender gender,
            @Size(max = 80) String homeCountry,
            Long universityId,
            @Size(max = 255) String languages,
            @Size(max = 2000) String bio,
            @Size(max = 1000) String avatarUrl) {
    }

    /** Full profile, returned only to the account owner. */
    public record UserResponse(
            Long id,
            String email,
            String fullName,
            Role role,
            String phone,
            Gender gender,
            String homeCountry,
            UniversityResponse university,
            String languages,
            String bio,
            String avatarUrl,
            boolean eduEmail,
            Instant createdAt) {

        public static UserResponse from(User u) {
            University uni = u.getUniversity();
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getPhone(),
                    u.getGender(), u.getHomeCountry(), uni == null ? null : UniversityResponse.from(uni),
                    u.getLanguages(), u.getBio(), u.getAvatarUrl(), u.isEduEmail(), u.getCreatedAt());
        }
    }

    /** Public view of another user: no email or phone until they choose to share it in chat. */
    public record PublicUserResponse(
            Long id,
            String fullName,
            Role role,
            String homeCountry,
            String universityName,
            String languages,
            String bio,
            String avatarUrl,
            boolean eduEmail) {

        public static PublicUserResponse from(User u) {
            return new PublicUserResponse(u.getId(), u.getFullName(), u.getRole(), u.getHomeCountry(),
                    u.getUniversity() == null ? null : u.getUniversity().getName(),
                    u.getLanguages(), u.getBio(), u.getAvatarUrl(), u.isEduEmail());
        }
    }
}
