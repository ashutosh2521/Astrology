package codes.ashutoshkumar.kundli.chart;

import codes.ashutoshkumar.kundli.config.KundliProperties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and reads birth charts. Owns the accuracy policies the spec assigns to the
 * backend (not the ephemeris service):
 *
 * <ul>
 *   <li><b>Local time → UTC resolution</b> via IANA tz rules, with an explicit warning
 *       for pre-1950 births where historical offsets (esp. India) may be unreliable.</li>
 *   <li><b>Boundary-case detection</b>: the ephemeris reports raw degrees-to-boundary;
 *       this service applies the configurable ~1° threshold and attaches warnings —
 *       a small birth-time error near a boundary can flip the whole match.</li>
 * </ul>
 */
@Service
public class BirthChartService {

    private final BirthChartRepository repository;
    private final EphemerisClient ephemeris;
    private final KundliProperties props;

    public BirthChartService(BirthChartRepository repository, EphemerisClient ephemeris,
                             KundliProperties props) {
        this.repository = repository;
        this.ephemeris = ephemeris;
        this.props = props;
    }

    public record CreateChartCommand(
            String label,
            String birthLocalDateTime,   // ISO local, e.g. 1990-05-15T14:53:00
            String timezone,             // IANA id, e.g. Asia/Kolkata
            double latitude,
            double longitude,
            String placeName
    ) {}

    @Transactional
    public BirthChart create(CreateChartCommand cmd) {
        LocalDateTime local = parseLocal(cmd.birthLocalDateTime());
        ZoneId zone = parseZone(cmd.timezone());
        Instant utc = local.atZone(zone).toInstant();

        List<String> warnings = new ArrayList<>();
        if (local.getYear() < props.historicalTzWarningBeforeYear()) {
            warnings.add(("Birth year %d predates reliable IANA timezone data (~%d); the "
                    + "UTC offset used may be wrong — verify against a historical offset table.")
                    .formatted(local.getYear(), props.historicalTzWarningBeforeYear()));
        }

        EphemerisClient.ComputedChart chart =
                ephemeris.computeChart(utc, cmd.latitude(), cmd.longitude());

        double threshold = props.boundaryWarningDegrees();
        if (chart.moonDegreesToNakshatraBoundary() < threshold) {
            warnings.add(("Moon is %.3f° from a Nakshatra boundary (threshold %.1f°): a small "
                    + "birth-time uncertainty could change the Nakshatra and the match result.")
                    .formatted(chart.moonDegreesToNakshatraBoundary(), threshold));
        }
        if (chart.moonDegreesToRashiBoundary() < threshold) {
            warnings.add(("Moon is %.3f° from a Rashi boundary (threshold %.1f°): a small "
                    + "birth-time uncertainty could change the Rashi and the match result.")
                    .formatted(chart.moonDegreesToRashiBoundary(), threshold));
        }

        BirthChart entity = new BirthChart(
                cmd.label(),
                local.toString(),
                zone.getId(),
                cmd.latitude(),
                cmd.longitude(),
                cmd.placeName(),
                utc.toString(),
                chart.moonRashiNumber(),
                chart.moonNakshatraNumber(),
                chart.moonPada(),
                chart.moonDegreesToRashiBoundary(),
                chart.moonDegreesToNakshatraBoundary(),
                chart.ascendantLongitude(),
                chart.marsLongitude(),
                chart.venusLongitude(),
                warnings.isEmpty() ? null : String.join("\n", warnings),
                chart.ayanamsa(),
                chart.precision(),
                chart.rawJson(),
                Instant.now().toString());
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public BirthChart get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Birth chart " + id + " not found"));
    }

    /**
     * The chart's label if it still exists, otherwise {@code null}. Unlike
     * {@link #get(long)} this never throws — read paths that display a stored
     * match (history list, result view) must tolerate a chart that has since
     * been deleted, falling back to a generic placeholder rather than 404ing
     * the whole list.
     */
    @Transactional(readOnly = true)
    public String findLabel(long id) {
        return repository.findById(id).map(BirthChart::getLabel).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<BirthChart> list() {
        return repository.findAll();
    }

    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Birth chart " + id + " not found");
        }
        repository.deleteById(id);
    }

    private static LocalDateTime parseLocal(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "birthLocalDateTime must be ISO-8601 local, e.g. 1990-05-15T14:53:00", e);
        }
    }

    private static ZoneId parseZone(String value) {
        try {
            return ZoneId.of(value);
        } catch (ZoneRulesException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "timezone must be an IANA id, e.g. Asia/Kolkata", e);
        }
    }
}
