package codes.ashutoshkumar.kundli.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Liveness for the whole calculation path: reports this app AND the ephemeris
 * service's own health (which re-asserts full Swiss Ephemeris precision). A degraded
 * ephemeris shows up here as DOWN rather than hiding until the first chart request.
 */
@RestController
public class HealthController {

    private final RestClient ephemerisRestClient;

    public HealthController(RestClient ephemerisRestClient) {
        this.ephemerisRestClient = ephemerisRestClient;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        String ephemeris;
        try {
            ephemerisRestClient.get().uri("/health").retrieve().toBodilessEntity();
            ephemeris = "UP";
        } catch (Exception e) {
            ephemeris = "DOWN: " + e.getMessage();
        }
        Map<String, String> body = Map.of("app", "UP", "ephemeris", ephemeris);
        return ephemeris.equals("UP")
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
