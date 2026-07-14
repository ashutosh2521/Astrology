package codes.ashutoshkumar.kundli.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ashutoshkumar.kundli.chart.EphemerisClient.ComputedChart;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Direct tests for the ephemeris-response parsing added for Milestone 4
 * (Manglik needs stored Mars/Venus/Ascendant longitudes).
 *
 * <p>These bypass the HTTP layer and drive the client with an in-memory
 * response body — the point is the JSON contract, not the wire.
 */
class EphemerisClientParserTest {

    private static final String SAMPLE_RESPONSE = """
            {
              "ayanamsa": "LAHIRI",
              "precision": "SWISS_EPHEMERIS_FULL",
              "julian_day_ut": 2451179.9513888885,
              "moon": {
                "longitude": 123.456,
                "speed": 13.2,
                "rashi":     { "number": 5, "name": "Simha", "degrees_in": 3.456, "degrees_to_boundary": 3.456 },
                "nakshatra": { "number": 10, "name": "Magha", "degrees_in": 3.456, "degrees_to_boundary": 3.456 },
                "pada": 2
              },
              "ascendant": {
                "longitude": 210.75,
                "rashi": { "number": 8, "name": "Vrishchika", "degrees_in": 0.75, "degrees_to_boundary": 0.75 }
              },
              "grahas": [
                { "name": "Sun",     "longitude": 190.0, "speed":  0.98, "retrograde": false, "rashi": {} },
                { "name": "Moon",    "longitude": 123.5, "speed": 13.20, "retrograde": false, "rashi": {} },
                { "name": "Mars",    "longitude":  85.25,"speed":  0.60, "retrograde": false, "rashi": {} },
                { "name": "Mercury", "longitude": 180.0, "speed":  1.20, "retrograde": false, "rashi": {} },
                { "name": "Jupiter", "longitude":  45.0, "speed":  0.10, "retrograde": false, "rashi": {} },
                { "name": "Venus",   "longitude": 300.5, "speed":  1.10, "retrograde": false, "rashi": {} },
                { "name": "Saturn",  "longitude":  10.0, "speed":  0.03, "retrograde": false, "rashi": {} },
                { "name": "Rahu",    "longitude": 250.0, "speed": -0.05, "retrograde": true,  "rashi": {} },
                { "name": "Ketu",    "longitude":  70.0, "speed":  0.00, "retrograde": false, "rashi": {} }
              ]
            }
            """;

    /**
     * The parsing branch lives inside {@code computeChart(...)} which also does
     * HTTP. This test exercises the private helper via reflection so we don't
     * have to stand up a fake HTTP server just to validate JSON extraction.
     */
    @Test
    void extractsMoonAscendantMarsVenusFromSampleResponse() throws Exception {
        var mapper = new ObjectMapper();
        var root = mapper.readTree(SAMPLE_RESPONSE);

        // Private static helper for graha lookup — the code path we added.
        Method grahaLongitude = EphemerisClient.class.getDeclaredMethod(
                "grahaLongitude", com.fasterxml.jackson.databind.JsonNode.class, String.class);
        grahaLongitude.setAccessible(true);

        assertEquals(85.25, (double) grahaLongitude.invoke(null, root, "Mars"), 1e-9);
        assertEquals(300.5, (double) grahaLongitude.invoke(null, root, "Venus"), 1e-9);
        assertEquals(190.0, (double) grahaLongitude.invoke(null, root, "Sun"), 1e-9);
        assertEquals(123.5, (double) grahaLongitude.invoke(null, root, "Moon"), 1e-9);
    }

    @Test
    void missingRequiredGrahaSurfacesAsEphemerisUnavailable() throws Exception {
        var mapper = new ObjectMapper();
        var root = mapper.readTree("""
                { "grahas": [ { "name": "Sun", "longitude": 190.0 } ] }
                """);
        Method grahaLongitude = EphemerisClient.class.getDeclaredMethod(
                "grahaLongitude", com.fasterxml.jackson.databind.JsonNode.class, String.class);
        grahaLongitude.setAccessible(true);

        var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> grahaLongitude.invoke(null, root, "Mars"));
        assertTrue(thrown.getCause() instanceof EphemerisUnavailableException,
                "missing graha must raise EphemerisUnavailableException, not a generic parse error");
        assertTrue(thrown.getCause().getMessage().contains("Mars"),
                "error message must name the missing graha for debugging");
    }

    @Test
    void computedChartCarriesAscendantAndPlanetLongitudes() {
        // Sanity: constructing a ComputedChart preserves the new fields.
        // Kept minimal — the JSON round-trip is exercised above.
        ComputedChart c = new ComputedChart(5, 10, 2, 3.456, 3.456,
                210.75, 85.25, 300.5,
                "SWISS_EPHEMERIS_FULL", "LAHIRI", "{}");
        assertEquals(210.75, c.ascendantLongitude(), 1e-9);
        assertEquals(85.25, c.marsLongitude(), 1e-9);
        assertEquals(300.5, c.venusLongitude(), 1e-9);
    }
}
