package codes.ashutoshkumar.kundli.profile;

import codes.ashutoshkumar.kundli.chart.BirthChart;
import codes.ashutoshkumar.kundli.chart.BirthChartService;
import codes.ashutoshkumar.kundli.chart.NotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes the single primary profile row.
 *
 * <p>Get returns an empty Optional on first run (nothing set yet) rather
 * than 404 — the "not configured" state is a first-class value the UI
 * uses to show the setup wizard, not an error.
 */
@Service
public class ProfileService {

    private final ProfileRepository repository;
    private final BirthChartService charts;

    public ProfileService(ProfileRepository repository, BirthChartService charts) {
        this.repository = repository;
        this.charts = charts;
    }

    /** Empty if the primary profile hasn't been set up yet. */
    @Transactional(readOnly = true)
    public Optional<Profile> get() {
        return repository.findById(Profile.KEY);
    }

    /**
     * Point the primary profile at a chart. The chart must already exist;
     * {@link NotFoundException} is thrown otherwise, so bad ids never end
     * up persisted on the profile row.
     */
    @Transactional
    public Profile setPrimaryChart(long chartId) {
        BirthChart chart = charts.get(chartId); // throws NotFoundException if absent
        String now = Instant.now().toString();
        Profile p = repository.findById(Profile.KEY)
                .orElseGet(() -> new Profile(chart.getId(), now));
        p.setPrimaryChartId(chart.getId());
        p.setUpdatedAt(now);
        return repository.save(p);
    }

    /**
     * Convenience for callers that need the primary chart directly.
     * Empty when either the profile isn't configured yet OR the chart it
     * points at has been deleted since setup.
     */
    @Transactional(readOnly = true)
    public Optional<BirthChart> primaryChart() {
        return get()
                .map(Profile::getPrimaryChartId)
                .flatMap(id -> {
                    try {
                        return Optional.of(charts.get(id));
                    } catch (NotFoundException e) {
                        return Optional.empty();
                    }
                });
    }
}
