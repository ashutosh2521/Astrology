package codes.ashutoshkumar.kundli.config;

import codes.ashutoshkumar.kundli.ashtakoot.AshtakootEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public RestClient ephemerisRestClient(KundliProperties props) {
        return RestClient.builder().baseUrl(props.ephemeris().baseUrl()).build();
    }
}
