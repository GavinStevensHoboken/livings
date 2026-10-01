package com.livings.geo;

public final class GeoUtils {

    public static final double EARTH_RADIUS_MILES = 3958.8;
    private static final double MILES_PER_DEGREE_LAT = 69.0;

    private GeoUtils() {
    }

    /** Great-circle distance in miles (haversine). */
    public static double distanceMiles(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_MILES * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Degrees of latitude spanned by {@code miles}. */
    public static double latDelta(double miles) {
        return miles / MILES_PER_DEGREE_LAT;
    }

    /** Degrees of longitude spanned by {@code miles} at the given latitude. */
    public static double lngDelta(double miles, double atLatitude) {
        return miles / (MILES_PER_DEGREE_LAT * Math.cos(Math.toRadians(atLatitude)));
    }
}
