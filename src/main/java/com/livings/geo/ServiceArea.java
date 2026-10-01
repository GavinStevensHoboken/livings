package com.livings.geo;

import java.util.regex.Pattern;

import com.livings.common.ApiException;

/** Guards that every address we store is inside the contiguous United States (+ DC). */
public final class ServiceArea {

    public static final String OUT_OF_AREA_MESSAGE =
            "Livings currently serves the contiguous United States and DC only. "
                    + "Alaska, Hawaii, Puerto Rico and other US territories are not supported.";

    private static final Pattern ZIP = Pattern.compile("^\\d{5}(-\\d{4})?$");

    // Generous bounding box around the lower 48 (Key West to the Northwest Angle, Cape Alava to Maine).
    private static final double MIN_LAT = 24.3;
    private static final double MAX_LAT = 49.5;
    private static final double MIN_LNG = -125.0;
    private static final double MAX_LNG = -66.8;

    private ServiceArea() {
    }

    public static String validateZip(String zip) {
        if (zip == null || !ZIP.matcher(zip.trim()).matches()) {
            throw ApiException.badRequest("ZIP code must be 5 digits (optionally ZIP+4)");
        }
        String normalized = zip.trim();
        int prefix = Integer.parseInt(normalized.substring(0, 3));
        boolean puertoRicoOrVirginIslands = prefix >= 6 && prefix <= 9;
        boolean hawaiiOrPacific = prefix >= 967 && prefix <= 969;
        boolean alaska = prefix >= 995;
        if (puertoRicoOrVirginIslands || hawaiiOrPacific || alaska) {
            throw ApiException.badRequest(OUT_OF_AREA_MESSAGE);
        }
        return normalized;
    }

    public static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw ApiException.badRequest("latitude and longitude must be provided together");
        }
        if (latitude < MIN_LAT || latitude > MAX_LAT || longitude < MIN_LNG || longitude > MAX_LNG) {
            throw ApiException.badRequest(OUT_OF_AREA_MESSAGE);
        }
    }
}
