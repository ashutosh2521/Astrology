package codes.ashutoshkumar.kundli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Accuracy-policy knobs live here in the backend — the ephemeris service reports raw
 * astronomy (e.g. degrees to a boundary); what counts as "too close" is our policy.
 */
@ConfigurationProperties(prefix = "kundli")
public record KundliProperties(
        Ephemeris ephemeris,
        double boundaryWarningDegrees,
        int historicalTzWarningBeforeYear
) {
    public record Ephemeris(String baseUrl) {}
}
