package codes.ashutoshkumar.kundli.monitoring;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Admin server for the Kundli deployment. It monitors the backend (which
 * registers as an Admin client) and its {@code ephemeris} health component, and emails
 * on status changes when SMTP is configured.
 *
 * <p>Runs as its own small process on {@code 127.0.0.1:9090} (see application.properties),
 * never exposed to the internet — reach the dashboard over an SSH tunnel. See
 * {@code infra/DEPLOY.md}.
 */
@SpringBootApplication
@EnableAdminServer
public class KundliMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(KundliMonitoringApplication.class, args);
    }
}
