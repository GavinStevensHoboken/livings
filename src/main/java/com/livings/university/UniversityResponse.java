package com.livings.university;

public record UniversityResponse(
        Long id,
        String name,
        String city,
        String state,
        double latitude,
        double longitude,
        String website) {

    public static UniversityResponse from(University u) {
        return new UniversityResponse(u.getId(), u.getName(), u.getCity(), u.getState().name(),
                u.getLatitude(), u.getLongitude(), u.getWebsite());
    }
}
