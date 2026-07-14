package codes.ashutoshkumar.kundli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Accuracy-policy knobs live here in the backend — the ephemeris service reports raw
 * astronomy (e.g. degrees to a boundary); what counts as "too close" is our policy.
 *
 * <p>The {@link Rules} block is the declared rule system every result carries with
 * it, so a stored match is always attributable to the exact convention that
 * produced it (spec: single declared, versioned rule system).
 */
@ConfigurationProperties(prefix = "kundli")
public record KundliProperties(
        Ephemeris ephemeris,
        double boundaryWarningDegrees,
        int historicalTzWarningBeforeYear,
        Rules rules
) {
    public record Ephemeris(String baseUrl) {}

    /**
     * Rule-system metadata surfaced on every match result. Kept as plain strings
     * (not enums) so bumps like "ashtakoot-v0-provisional" → "ashtakoota-v1.0"
     * are config-only and stored historical results retain their exact version.
     *
     * @param ayanamsa              e.g. LAHIRI
     * @param matchingSystem        e.g. NORTH_INDIAN_ASHTAKOOTA
     * @param ashtakootaRuleVersion e.g. ashtakoot-v0-provisional
     * @param manglikRuleVersion    e.g. not-implemented until Milestone 4 lands
     * @param ephemerisMode         e.g. SWISS_EPHEMERIS_FULL
     */
    public record Rules(
            String ayanamsa,
            String matchingSystem,
            String ashtakootaRuleVersion,
            String manglikRuleVersion,
            String ephemerisMode
    ) {}
}
