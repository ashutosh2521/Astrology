package codes.ashutoshkumar.kundli.match;

import codes.ashutoshkumar.kundli.ashtakoot.AshtakootEngine;
import codes.ashutoshkumar.kundli.ashtakoot.AshtakootResult;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import codes.ashutoshkumar.kundli.chart.BirthChart;
import codes.ashutoshkumar.kundli.chart.BirthChartService;
import codes.ashutoshkumar.kundli.chart.NotFoundException;
import codes.ashutoshkumar.kundli.manglik.ManglikCompatibility;
import codes.ashutoshkumar.kundli.manglik.ManglikEngine;
import codes.ashutoshkumar.kundli.manglik.ManglikStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs Ashtakoot matches over stored charts. Matching is pure table-driven Java —
 * zero calls to the ephemeris service (charts were computed and cached at profile
 * creation). The engine result is persisted verbatim alongside a rules version.
 */
@Service
public class MatchService {

    /**
     * Bump when any reference table or cancellation convention changes, so stored
     * results are attributable to the rule set that produced them. "provisional"
     * marks results computed before Drik Panchang verification completes.
     */
    public static final String RULES_VERSION = "ashtakoot-v0-provisional";

    private final MatchRepository repository;
    private final BirthChartService charts;
    private final AshtakootEngine engine;
    private final ManglikEngine manglikEngine;
    private final ObjectMapper mapper;

    public MatchService(MatchRepository repository, BirthChartService charts,
                        AshtakootEngine engine, ManglikEngine manglikEngine,
                        ObjectMapper mapper) {
        this.repository = repository;
        this.charts = charts;
        this.engine = engine;
        this.manglikEngine = manglikEngine;
        this.mapper = mapper;
    }

    /**
     * Both engine outputs from a single match run. {@link #manglik} is
     * {@code null} only when one of the charts predates the M2 entity
     * extension and lacks the longitudes Manglik needs; new charts
     * always populate them, so this null case is a legacy read path.
     */
    public record MatchOutcome(MatchRecord record, AshtakootResult result,
                               ManglikCompatibility manglik,
                               BirthChart boy, BirthChart girl) {}

    @Transactional
    public MatchOutcome match(long boyChartId, long girlChartId) {
        BirthChart boy = charts.get(boyChartId);
        BirthChart girl = charts.get(girlChartId);

        AshtakootResult result = engine.match(toMoonChart(boy), toMoonChart(girl));
        ManglikCompatibility manglik = computeManglik(boy, girl);

        MatchRecord record = new MatchRecord(
                boy.getId(), girl.getId(),
                result.totalPoints(), result.maxPoints(), result.verdict(),
                toJson(result), RULES_VERSION,
                manglik == null ? null : toJson(manglik),
                manglik == null ? null : manglik.ruleVersion(),
                Instant.now().toString());
        return new MatchOutcome(repository.save(record), result, manglik, boy, girl);
    }

    /**
     * Compute Manglik if both charts carry the longitudes Manglik needs.
     * Returns null when either chart is missing them (legacy pre-M2 rows) —
     * the DTO layer then omits {@code manglik} from the response rather
     * than fabricating a placeholder or a partial result.
     */
    private ManglikCompatibility computeManglik(BirthChart boy, BirthChart girl) {
        if (!hasManglikInputs(boy) || !hasManglikInputs(girl)) {
            return null;
        }
        ManglikStatus boyManglik = manglikEngine.evaluate(
                boy.getAscendantLongitude(), boy.getMoonRashiNumber(),
                boy.getMarsLongitude(), boy.getVenusLongitude());
        ManglikStatus girlManglik = manglikEngine.evaluate(
                girl.getAscendantLongitude(), girl.getMoonRashiNumber(),
                girl.getMarsLongitude(), girl.getVenusLongitude());
        return manglikEngine.combine(boyManglik, girlManglik);
    }

    private static boolean hasManglikInputs(BirthChart c) {
        return c.getAscendantLongitude() != null
                && c.getMarsLongitude() != null
                && c.getVenusLongitude() != null;
    }

    @Transactional(readOnly = true)
    public MatchRecord get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Match " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<MatchRecord> list() {
        return repository.findAll();
    }

    private static MoonChart toMoonChart(BirthChart c) {
        return new MoonChart(
                Rashi.ofNumber(c.getMoonRashiNumber()),
                Nakshatra.ofNumber(c.getMoonNakshatraNumber()),
                c.getMoonPada());
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize match payload", e);
        }
    }
}
