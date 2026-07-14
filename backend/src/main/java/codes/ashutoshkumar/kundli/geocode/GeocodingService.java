package codes.ashutoshkumar.kundli.geocode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Resolves a typed birthplace to coordinates + timezone via OpenStreetMap
 * Nominatim, so any village or town — not just the bundled list — can be picked
 * without the user ever knowing latitude/longitude.
 *
 * <p>Failure is non-fatal by design: if the geocoder is unreachable, disabled,
 * or returns nothing, we return an empty list. The frontend still has its
 * bundled matches and the manual-entry escape hatch, so a lookup outage never
 * blocks the form.
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final RestClient rest;
    private final ObjectMapper mapper;
    private final GeocodingProperties props;

    public GeocodingService(RestClient geocodingRestClient, ObjectMapper mapper, GeocodingProperties props) {
        this.rest = geocodingRestClient;
        this.mapper = mapper;
        this.props = props;
    }

    /** Search for places matching {@code query}; never throws — returns [] on any failure. */
    public List<GeoResult> search(String query) {
        String q = query == null ? "" : query.trim();
        if (!props.enabled() || q.length() < 3) {
            return List.of();
        }
        String uri = UriComponentsBuilder.fromPath("/search")
                .queryParam("q", q)
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("limit", props.limit())
                .queryParam("accept-language", "en")
                .build()
                .toUriString();

        String raw;
        try {
            raw = rest.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException e) {
            // Online lookup is best-effort; the bundled list and manual entry remain.
            log.warn("Geocoding lookup failed for '{}': {}", q, e.getMessage());
            return List.of();
        }
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        try {
            JsonNode arr = mapper.readTree(raw);
            List<GeoResult> results = new ArrayList<>();
            for (JsonNode node : arr) {
                GeoResult r = toResult(node);
                if (r != null) {
                    results.add(r);
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Could not parse geocoding response for '{}': {}", q, e.getMessage());
            return List.of();
        }
    }

    private GeoResult toResult(JsonNode node) {
        double lat = node.path("lat").asDouble(Double.NaN);
        double lon = node.path("lon").asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }
        JsonNode address = node.path("address");
        String name = firstNonBlank(
                node.path("name").asText(null),
                firstSegment(node.path("display_name").asText(null)));
        if (name == null) {
            return null;
        }
        String region = firstNonBlank(
                address.path("state").asText(null),
                address.path("county").asText(null),
                address.path("country").asText(null),
                "");
        String tz = timezoneFor(address.path("country_code").asText(null), lon);
        return new GeoResult(name, region, lat, lon, tz);
    }

    /**
     * Best-effort IANA timezone. India — the primary audience — is a single zone
     * with no DST, so {@code in} is always exact. Diaspora countries already in
     * the app's world list get their canonical zone. Anything else falls back to
     * a fixed longitude-derived offset (no DST); the advanced form's manual
     * timezone selector remains the precise override for those edge cases.
     */
    static String timezoneFor(String countryCode, double longitude) {
        if (countryCode != null) {
            String tz = COUNTRY_TZ.get(countryCode.toLowerCase());
            if (tz != null) {
                return tz;
            }
        }
        long offset = Math.round(longitude / 15.0);
        if (offset == 0) {
            return "Etc/GMT";
        }
        // IANA "Etc/GMT" offsets are sign-inverted: Etc/GMT-5 == UTC+5.
        return offset > 0 ? "Etc/GMT-" + offset : "Etc/GMT+" + (-offset);
    }

    private static final Map<String, String> COUNTRY_TZ = Map.ofEntries(
            Map.entry("in", "Asia/Kolkata"),
            Map.entry("np", "Asia/Kathmandu"),
            Map.entry("lk", "Asia/Colombo"),
            Map.entry("bd", "Asia/Dhaka"),
            Map.entry("pk", "Asia/Karachi"),
            Map.entry("ae", "Asia/Dubai"),
            Map.entry("qa", "Asia/Qatar"),
            Map.entry("sa", "Asia/Riyadh"),
            Map.entry("om", "Asia/Muscat"),
            Map.entry("kw", "Asia/Kuwait"),
            Map.entry("bh", "Asia/Bahrain"),
            Map.entry("sg", "Asia/Singapore"),
            Map.entry("my", "Asia/Kuala_Lumpur"),
            Map.entry("th", "Asia/Bangkok"),
            Map.entry("hk", "Asia/Hong_Kong"),
            Map.entry("jp", "Asia/Tokyo"),
            Map.entry("au", "Australia/Sydney"),
            Map.entry("nz", "Pacific/Auckland"),
            Map.entry("gb", "Europe/London"),
            Map.entry("fr", "Europe/Paris"),
            Map.entry("de", "Europe/Berlin"),
            Map.entry("nl", "Europe/Amsterdam"),
            Map.entry("ca", "America/Toronto"),
            Map.entry("us", "America/New_York"),
            Map.entry("ke", "Africa/Nairobi"),
            Map.entry("za", "Africa/Johannesburg"),
            Map.entry("mu", "Indian/Mauritius"));

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String firstSegment(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        int comma = displayName.indexOf(',');
        return comma > 0 ? displayName.substring(0, comma).trim() : displayName.trim();
    }
}
