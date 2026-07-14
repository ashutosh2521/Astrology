package codes.ashutoshkumar.kundli.geocode;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the online place lookup (birthplace search fallback).
 *
 * <p>The bundled city list ({@code web/.../cities.ts}) covers common towns for
 * instant, offline results — but it can never list every village. When a user's
 * birthplace isn't bundled, the frontend asks the backend to geocode the typed
 * text; the backend proxies an online geocoder so we can set a proper
 * User-Agent (the public OpenStreetMap Nominatim usage policy requires one),
 * keep the third-party host out of the browser (CORS), and attach a timezone.
 *
 * <p>Kept as its own {@code @ConfigurationProperties} class rather than folded
 * into {@link codes.ashutoshkumar.kundli.config.KundliProperties} so its
 * positional record constructor (used directly in tests) stays untouched.
 *
 * @param baseUrl   geocoder base URL (default: public Nominatim)
 * @param userAgent identifying User-Agent sent on every request, per Nominatim policy
 * @param limit     maximum results requested per query
 * @param enabled   master switch; when false {@code /api/geocode} returns an empty list
 */
@ConfigurationProperties(prefix = "kundli.geocoding")
public record GeocodingProperties(
        String baseUrl,
        String userAgent,
        int limit,
        boolean enabled
) {
    public GeocodingProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://nominatim.openstreetmap.org";
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "AstrologyKundliApp/1.0 (github.com/ashutosh2521/astrology)";
        }
        if (limit <= 0) {
            limit = 8;
        }
    }
}
