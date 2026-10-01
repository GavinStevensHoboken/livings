package com.livings.geo;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.livings.common.ApiException;

/**
 * The service area: the 48 contiguous states plus the District of Columbia.
 * Alaska, Hawaii, Puerto Rico and the other US territories are intentionally absent.
 */
public enum UsState {
    AL("Alabama"), AZ("Arizona"), AR("Arkansas"), CA("California"), CO("Colorado"),
    CT("Connecticut"), DE("Delaware"), DC("District of Columbia"), FL("Florida"), GA("Georgia"),
    ID("Idaho"), IL("Illinois"), IN("Indiana"), IA("Iowa"), KS("Kansas"),
    KY("Kentucky"), LA("Louisiana"), ME("Maine"), MD("Maryland"), MA("Massachusetts"),
    MI("Michigan"), MN("Minnesota"), MS("Mississippi"), MO("Missouri"), MT("Montana"),
    NE("Nebraska"), NV("Nevada"), NH("New Hampshire"), NJ("New Jersey"), NM("New Mexico"),
    NY("New York"), NC("North Carolina"), ND("North Dakota"), OH("Ohio"), OK("Oklahoma"),
    OR("Oregon"), PA("Pennsylvania"), RI("Rhode Island"), SC("South Carolina"), SD("South Dakota"),
    TN("Tennessee"), TX("Texas"), UT("Utah"), VT("Vermont"), VA("Virginia"),
    WA("Washington"), WV("West Virginia"), WI("Wisconsin"), WY("Wyoming");

    /** Codes users may reasonably try that we explicitly do not serve, for a clearer error. */
    public static final Set<String> UNSUPPORTED = Set.of("AK", "HI", "PR", "VI", "GU", "AS", "MP", "UM");

    private static final Map<String, UsState> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    private final String displayName;

    UsState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Resolves a two-letter code, rejecting anything outside the service area with a 400. */
    public static UsState fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("State is required");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        UsState state = BY_CODE.get(normalized);
        if (state != null) {
            return state;
        }
        if (UNSUPPORTED.contains(normalized)) {
            throw ApiException.badRequest(ServiceArea.OUT_OF_AREA_MESSAGE);
        }
        throw ApiException.badRequest("Unknown state code: " + code);
    }
}
