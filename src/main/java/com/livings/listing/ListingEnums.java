package com.livings.listing;

/** Namespace for the small enums that describe a listing. */
public final class ListingEnums {

    private ListingEnums() {
    }

    public enum Category {
        /** A whole unit (or room) leased directly from a landlord / property manager. */
        PRIVATE_RENTAL,
        /** A room in a shared home with existing roommates (incl. student sublets and lease takeovers). */
        ROOMMATE
    }

    public enum RoomType {
        ENTIRE_PLACE, PRIVATE_ROOM, SHARED_ROOM
    }

    public enum PropertyType {
        APARTMENT, HOUSE, CONDO, TOWNHOUSE, STUDIO, STUDENT_HOUSING
    }

    public enum GenderPreference {
        ANY, FEMALE_ONLY, MALE_ONLY
    }

    public enum Status {
        ACTIVE, PAUSED, RENTED, ARCHIVED
    }

    public enum Amenity {
        IN_UNIT_LAUNDRY, LAUNDRY_IN_BUILDING, PARKING, AIR_CONDITIONING, HEATING, DISHWASHER,
        WIFI_INCLUDED, GYM, POOL, ELEVATOR, DOORMAN, PRIVATE_BATHROOM, STUDY_DESK,
        NEAR_PUBLIC_TRANSIT, OUTDOOR_SPACE, WHEELCHAIR_ACCESSIBLE
    }
}
