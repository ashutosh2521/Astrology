package codes.ashutoshkumar.kundli.chart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for the internal Python ephemeris service — the Java side of the contract:
 * we send a resolved UTC instant, coordinates, and an EXPLICIT ayanamsa (never a
 * server-side default); we get back sidereal chart data plus a precision audit field.
 *
 * <p>Parses only what the backend consumes (Moon placement, precision) and keeps the
 * full response as raw JSON for storage, so later features (Lagna, Mangal Dosha) can
 * read the stored charts without recompute or a client change.
 */
@Component
public class EphemerisClient {

    /** Locked-in convention for Vedic matching; a change here is a visible code change. */
    public static final String AYANAMSA = "LAHIRI";
    private static final String PRECISION_FULL = "SWISS_EPHEMERIS_FULL";

    private final RestClient rest;
    private final ObjectMapper mapper;

    public EphemerisClient(RestClient ephemerisRestClient, ObjectMapper mapper) {
        this.rest = ephemerisRestClient;
        this.mapper = mapper;
    }

    /** Moon placement extracted from a chart response, plus the raw JSON for storage. */
    public record ComputedChart(
            int moonRashiNumber,
            int moonNakshatraNumber,
            int moonPada,
            double moonDegreesToRashiBoundary,
            double moonDegreesToNakshatraBoundary,
            String precision,
            String ayanamsa,
            String rawJson
    ) {}

    public ComputedChart computeChart(Instant utc, double latitude, double longitude) {
        // Pre-serialize so the request goes out with a Content-Length body (fixed-length
        // HTTP/1.1). Streaming converters produce Transfer-Encoding: chunked, which —
        // combined with the JDK client's h2c upgrade attempt — breaks some uvicorn
        // versions into dropping the body entirely (422 "body missing").
        String payload;
        try {
            payload = mapper.writeValueAsString(Map.of(
                    "utc", utc.toString(),
                    "latitude", latitude,
                    "longitude", longitude,
                    "ayanamsa", AYANAMSA));
        } catch (Exception e) {
            throw new EphemerisUnavailableException("Could not serialize chart request", e);
        }

        String raw;
        try {
            raw = rest.post()
                    .uri("/chart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new EphemerisUnavailableException(
                    "Ephemeris service call failed: " + e.getMessage(), e);
        }

        try {
            JsonNode root = mapper.readTree(raw);
            String precision = root.path("precision").asText();
            // Defense in depth: the service already refuses Moshier fallback, but a chart
            // must never be stored unless full precision is positively confirmed.
            if (!PRECISION_FULL.equals(precision)) {
                throw new EphemerisUnavailableException(
                        "Ephemeris returned non-full precision '" + precision + "'; refusing chart");
            }
            JsonNode moon = root.path("moon");
            return new ComputedChart(
                    moon.path("rashi").path("number").asInt(),
                    moon.path("nakshatra").path("number").asInt(),
                    moon.path("pada").asInt(),
                    moon.path("rashi").path("degrees_to_boundary").asDouble(),
                    moon.path("nakshatra").path("degrees_to_boundary").asDouble(),
                    precision,
                    root.path("ayanamsa").asText(),
                    raw);
        } catch (EphemerisUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new EphemerisUnavailableException(
                    "Could not parse ephemeris response: " + e.getMessage(), e);
        }
    }
}
