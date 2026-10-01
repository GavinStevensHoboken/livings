package com.livings.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.livings.common.ApiException;

class ServiceAreaTest {

    @ParameterizedTest
    @ValueSource(strings = {"AK", "HI", "PR", "pr", "VI", "GU"})
    void rejectsStatesOutsideServiceArea(String code) {
        assertThatThrownBy(() -> UsState.fromCode(code))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("contiguous United States");
    }

    @Test
    void acceptsContiguousStatesAndDc() {
        assertThat(UsState.fromCode("ma")).isEqualTo(UsState.MA);
        assertThat(UsState.fromCode("DC")).isEqualTo(UsState.DC);
        assertThat(UsState.values()).hasSize(49);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00901", "00802", "96813", "96910", "99501", "99950"})
    void rejectsAlaskaHawaiiAndTerritoryZips(String zip) {
        assertThatThrownBy(() -> ServiceArea.validateZip(zip)).hasMessageContaining("contiguous United States");
    }

    @ParameterizedTest
    @ValueSource(strings = {"02115", "10012", "94720", "98105", "33139", "20057", "02115-1234"})
    void acceptsContiguousZips(String zip) {
        assertThat(ServiceArea.validateZip(zip)).isEqualTo(zip);
    }

    @Test
    void rejectsCoordinatesInHawaiiAndAlaska() {
        assertThatThrownBy(() -> ServiceArea.validateCoordinates(21.3069, -157.8583)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ServiceArea.validateCoordinates(61.2181, -149.9003)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ServiceArea.validateCoordinates(18.4655, -66.1057)).isInstanceOf(ApiException.class);
        ServiceArea.validateCoordinates(42.3398, -71.0892);
        ServiceArea.validateCoordinates(null, null);
    }

    @Test
    void distanceIsReasonable() {
        // Northeastern -> Boston University is roughly 1 mile.
        double d = GeoUtils.distanceMiles(42.3398, -71.0892, 42.3505, -71.1054);
        assertThat(d).isBetween(0.8, 1.5);
    }
}
