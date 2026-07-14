package codes.ashutoshkumar.kundli.geocode;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Birthplace lookup endpoint. The place-picker calls this when the bundled
 * city list doesn't cover what the user typed, so small towns and villages
 * still resolve to coordinates + timezone automatically.
 */
@RestController
public class GeocodingController {

    private final GeocodingService service;

    public GeocodingController(GeocodingService service) {
        this.service = service;
    }

    @GetMapping("/api/geocode")
    public List<GeoResult> geocode(@RequestParam("q") String query) {
        return service.search(query);
    }
}
