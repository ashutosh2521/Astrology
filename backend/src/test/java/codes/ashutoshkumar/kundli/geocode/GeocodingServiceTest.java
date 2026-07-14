package codes.ashutoshkumar.kundli.geocode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeocodingServiceTest {

    @Test
    void indiaAlwaysResolvesToKolkata() {
        // India is a single zone with no DST — always exact regardless of longitude.
        assertThat(GeocodingService.timezoneFor("in", 85.13)).isEqualTo("Asia/Kolkata");
        assertThat(GeocodingService.timezoneFor("IN", 77.20)).isEqualTo("Asia/Kolkata");
    }

    @Test
    void knownDiasporaCountriesUseCanonicalZone() {
        assertThat(GeocodingService.timezoneFor("ae", 55.27)).isEqualTo("Asia/Dubai");
        assertThat(GeocodingService.timezoneFor("np", 85.32)).isEqualTo("Asia/Kathmandu");
        assertThat(GeocodingService.timezoneFor("us", -74.0)).isEqualTo("America/New_York");
    }

    @Test
    void unknownCountryFallsBackToLongitudeOffset() {
        // IANA Etc/GMT offsets are sign-inverted: Etc/GMT-5 == UTC+5.
        assertThat(GeocodingService.timezoneFor("zz", 75.0)).isEqualTo("Etc/GMT-5");
        assertThat(GeocodingService.timezoneFor(null, -60.0)).isEqualTo("Etc/GMT+4");
        assertThat(GeocodingService.timezoneFor(null, 0.0)).isEqualTo("Etc/GMT");
    }
}
