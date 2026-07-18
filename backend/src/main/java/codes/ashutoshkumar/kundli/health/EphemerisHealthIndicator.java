package codes.ashutoshkumar.kundli.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Surfaces the internal ephemeris service as an Actuator health component named
 * {@code ephemeris}, so it appears in {@code /actuator/health} and in the Spring Boot
 * Admin dashboard, and — because a DOWN component pulls overall health DOWN — triggers
 * the Admin server's email alert.
 *
 * <p>The ephemeris {@code /health} returns 503 if it ever degrades from full Swiss
 * Ephemeris precision to Moshier fallback; that surfaces here as DOWN, so the accuracy
 * guarantee is monitored, not just liveness.
 */
@Component
public class EphemerisHealthIndicator implements HealthIndicator {

    private final RestClient ephemerisRestClient;

    // Matches the bean by name (see AppConfig#ephemerisRestClient), same as HealthController.
    public EphemerisHealthIndicator(RestClient ephemerisRestClient) {
        this.ephemerisRestClient = ephemerisRestClient;
    }

    @Override
    public Health health() {
        try {
            ephemerisRestClient.get().uri("/health").retrieve().toBodilessEntity();
            return Health.up().withDetail("service", "ephemeris").build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "ephemeris")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
