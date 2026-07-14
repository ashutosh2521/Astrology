package codes.ashutoshkumar.kundli.config;

import codes.ashutoshkumar.kundli.ashtakoot.AshtakootEngine;
import codes.ashutoshkumar.kundli.manglik.ManglikEngine;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KundliProperties.class)
public class AppConfig {

    /**
     * The engine loads and strictly validates all reference tables at startup, so a
     * broken table fails app boot rather than a match request.
     */
    @Bean
    public AshtakootEngine ashtakootEngine() {
        return AshtakootEngine.create();
    }

    /**
     * Manglik engine — loads and validates {@code /manglik/manglik_rules.json}
     * at construction, same "fail fast at boot" stance the Ashtakoot engine
     * uses for its reference tables. See {@link ManglikEngine#create()}.
     */
    @Bean
    public ManglikEngine manglikEngine() {
        return ManglikEngine.create();
    }

    /**
     * Client for the internal ephemeris service. Deliberately uses the buffering
     * HttpURLConnection factory: plain HTTP/1.1 with a Content-Length body. The
     * default JDK HttpClient attempts an h2c upgrade ({@code Upgrade: h2c} +
     * chunked body) on cleartext HTTP, which uvicorn doesn't support — some
     * uvicorn/h11 versions then drop the body and FastAPI 422s with "body missing".
     */
    @Bean
    public RestClient ephemerisRestClient(KundliProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder()
                .baseUrl(props.ephemeris().baseUrl())
                .requestFactory(factory)
                .build();
    }
}
