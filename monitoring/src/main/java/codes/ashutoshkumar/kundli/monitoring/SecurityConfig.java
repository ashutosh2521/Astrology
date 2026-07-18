package codes.ashutoshkumar.kundli.monitoring;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Secures the Admin dashboard with form login (credentials come from
 * {@code spring.security.user.*}, set via env). The client-registration and Actuator
 * endpoints stay open so the backend can register and be scraped — safe because the
 * whole server is bound to loopback (see application.properties).
 *
 * <p>Adapted from the Spring Boot Admin reference security configuration.
 */
@Configuration
public class SecurityConfig {

    private final AdminServerProperties adminServer;

    public SecurityConfig(AdminServerProperties adminServer) {
        this.adminServer = adminServer;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServer.path("/"));

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(this.adminServer.path("/assets/**")).permitAll()
                        .requestMatchers(this.adminServer.path("/actuator/info")).permitAll()
                        .requestMatchers(this.adminServer.path("/login")).permitAll()
                        // Client registration + instance polling — loopback-only, so open.
                        .requestMatchers(this.adminServer.path("/instances")).permitAll()
                        .requestMatchers(this.adminServer.path("/instances/*")).permitAll()
                        .requestMatchers(this.adminServer.path("/actuator/**")).permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage(this.adminServer.path("/login"))
                        .successHandler(successHandler))
                .logout(logout -> logout.logoutUrl(this.adminServer.path("/logout")))
                .httpBasic(Customizer.withDefaults())
                // These endpoints are machine-to-machine (no browser CSRF token).
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        this.adminServer.path("/instances"),
                        this.adminServer.path("/instances/*"),
                        this.adminServer.path("/actuator/**")));

        return http.build();
    }
}
