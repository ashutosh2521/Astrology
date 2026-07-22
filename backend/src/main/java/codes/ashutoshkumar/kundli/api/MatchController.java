package codes.ashutoshkumar.kundli.api;

import codes.ashutoshkumar.kundli.ashtakoot.AshtakootResult;
import codes.ashutoshkumar.kundli.ashtakoot.RecommendationCategory;
import codes.ashutoshkumar.kundli.attributes.KundliAttributes;
import codes.ashutoshkumar.kundli.attributes.KundliAttributesService;
import codes.ashutoshkumar.kundli.chart.BirthChart;
import codes.ashutoshkumar.kundli.chart.BirthChartService;
import codes.ashutoshkumar.kundli.config.KundliProperties;
import codes.ashutoshkumar.kundli.manglik.ManglikCompatibility;
import codes.ashutoshkumar.kundli.match.MatchRecord;
import codes.ashutoshkumar.kundli.match.MatchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    public record CreateMatchRequest(@NotNull Long boyChartId, @NotNull Long girlChartId) {}

    /**
     * The declared rule system a result was produced under. Same shape on every
     * match response — makes stored results independently attributable and lets
     * a future recompute-with-newer-rules feature diff two blocks unambiguously.
     */
    public record RuleMetadata(
            String ayanamsa,
            String matchingSystem,
            String ashtakootaRuleVersion,
            String manglikRuleVersion,
            String ephemerisMode
    ) {
        static RuleMetadata from(KundliProperties.Rules r) {
            return new RuleMetadata(r.ayanamsa(), r.matchingSystem(),
                    r.ashtakootaRuleVersion(), r.manglikRuleVersion(), r.ephemerisMode());
        }
    }

    /**
     * A stored match, with the full engine results re-inflated from JSON.
     * {@link #rulesVersion} is kept for backward compatibility with existing
     * clients; {@link #ruleMetadata} is the structured block the spec asks
     * every result to expose.
     *
     * <p>{@link #manglik} is {@code null} only for legacy records created
     * before M4 (or from BirthCharts that predate M2's Ascendant/Mars/Venus
     * columns). Every new match populates it.
     */
    public record MatchResponse(
            long id,
            long boyChartId,
            long girlChartId,
            String boyLabel,
            String girlLabel,
            AshtakootResult result,
            ManglikCompatibility manglik,
            /**
             * Preliminary-match category from the configured thresholds:
             * WEAK / MODERATE / STRONG. Derived from the total score; the
             * Mother-mode UI renders it as the top-line Hindi banner.
             */
            RecommendationCategory recommendation,
            String rulesVersion,
            RuleMetadata ruleMetadata,
            String createdAt,
            /** Kundli attributes for the boy (Gana, Nadi, Yoni, 7th house …). Null if chart was deleted. */
            KundliAttributes boyAttributes,
            /** Kundli attributes for the girl (Gana, Nadi, Yoni, 7th house …). Null if chart was deleted. */
            KundliAttributes girlAttributes
    ) {}

    private final MatchService service;
    private final BirthChartService charts;
    private final KundliAttributesService attributesService;
    private final ObjectMapper mapper;
    private final KundliProperties props;

    public MatchController(MatchService service, BirthChartService charts,
                           KundliAttributesService attributesService,
                           ObjectMapper mapper, KundliProperties props) {
        this.service = service;
        this.charts = charts;
        this.attributesService = attributesService;
        this.mapper = mapper;
        this.props = props;
    }

    /** Current declared rule system — attached to results computed under it. */
    private RuleMetadata currentMetadata() {
        return RuleMetadata.from(props.rules());
    }

    /**
     * Metadata for a stored result. The historical {@code ashtakootaRuleVersion}
     * and {@code manglikRuleVersion} come from the frozen values on {@link MatchRecord}
     * so a historical match keeps its exact versions even after a config bump.
     * Other fields (ayanamsa, ephemeris mode) are not yet stored per-record;
     * until we freeze the full block on the entity, current values are reported
     * for those — fine while the whole block is one set.
     */
    private RuleMetadata metadataFor(MatchRecord r) {
        KundliProperties.Rules current = props.rules();
        String manglikVersion = r.getManglikRulesVersion() == null
                ? current.manglikRuleVersion()
                : r.getManglikRulesVersion();
        return new RuleMetadata(
                current.ayanamsa(), current.matchingSystem(),
                r.getRulesVersion(), manglikVersion, current.ephemerisMode());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse create(@jakarta.validation.Valid @RequestBody CreateMatchRequest req) {
        MatchService.MatchOutcome outcome = service.match(req.boyChartId(), req.girlChartId());
        MatchRecord r = outcome.record();
        return new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                outcome.boy().getLabel(), outcome.girl().getLabel(),
                outcome.result(), outcome.manglik(), outcome.recommendation(),
                r.getRulesVersion(), currentMetadata(), r.getCreatedAt(),
                attributesService.derive(outcome.boy()),
                attributesService.derive(outcome.girl()));
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable long id) {
        MatchRecord r = service.get(id);
        return toResponse(r);
    }

    @GetMapping
    public List<MatchResponse> list() {
        return service.list().stream().map(this::toResponse).toList();
    }

    /**
     * Re-inflate a stored match into a response. The bride/groom labels and Kundli
     * attributes are resolved from their charts here (not persisted on the match) so
     * the history list and result view show the real names and attribute data. If a
     * chart was deleted after the match was stored, labels/attributes gracefully degrade
     * to null rather than 404-ing the whole list.
     */
    private MatchResponse toResponse(MatchRecord r) {
        BirthChart boy = charts.findChart(r.getBoyChartId());
        BirthChart girl = charts.findChart(r.getGirlChartId());
        return new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                boy != null ? boy.getLabel() : null,
                girl != null ? girl.getLabel() : null,
                parse(r.getResultJson()),
                parseManglik(r.getManglikJson()),
                service.categorize(r.getTotalPoints()),
                r.getRulesVersion(), metadataFor(r), r.getCreatedAt(),
                boy != null ? attributesService.derive(boy) : null,
                girl != null ? attributesService.derive(girl) : null);
    }

    private AshtakootResult parse(String json) {
        try {
            return mapper.readValue(json, AshtakootResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored match result is unreadable", e);
        }
    }

    /** Nullable — returns null when the stored record had no Manglik JSON (legacy). */
    private ManglikCompatibility parseManglik(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, ManglikCompatibility.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored Manglik result is unreadable", e);
        }
    }
}
