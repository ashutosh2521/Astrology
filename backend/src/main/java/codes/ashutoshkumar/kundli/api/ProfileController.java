package codes.ashutoshkumar.kundli.api;

import codes.ashutoshkumar.kundli.api.ChartDtos.ChartResponse;
import codes.ashutoshkumar.kundli.chart.BirthChart;
import codes.ashutoshkumar.kundli.profile.Profile;
import codes.ashutoshkumar.kundli.profile.ProfileService;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Mother-mode primary profile.
 *
 * <p>GET returns {@code configured: false} on first run rather than 404 —
 * the "not configured yet" state is a first-class UI branch (the setup
 * wizard on the Mother-mode home). PUT is idempotent: it points the
 * primary profile at an existing chart id.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    public record ProfileResponse(
            boolean configured,
            /** The chart id the profile currently points at; null when configured=false. */
            Long primaryChartId,
            /** The full primary chart, inflated; null when configured=false. */
            ChartResponse primaryChart,
            String updatedAt
    ) {
        static ProfileResponse notConfigured() {
            return new ProfileResponse(false, null, null, null);
        }

        static ProfileResponse of(Profile p, BirthChart chart) {
            return new ProfileResponse(
                    true,
                    p.getPrimaryChartId(),
                    chart == null ? null : ChartResponse.from(chart),
                    p.getUpdatedAt());
        }
    }

    public record SetPrimaryRequest(@NotNull Long chartId) {}

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileResponse get() {
        Optional<Profile> p = service.get();
        if (p.isEmpty()) {
            return ProfileResponse.notConfigured();
        }
        // primaryChart() re-resolves the chart id in case it was deleted since
        // setup — returning configured=true with a null primaryChart tells the
        // UI "the pointer is set but its target vanished".
        return ProfileResponse.of(p.get(), service.primaryChart().orElse(null));
    }

    @PutMapping
    public ProfileResponse setPrimary(@jakarta.validation.Valid @RequestBody SetPrimaryRequest req) {
        Profile updated = service.setPrimaryChart(req.chartId());
        return ProfileResponse.of(updated, service.primaryChart().orElse(null));
    }
}
