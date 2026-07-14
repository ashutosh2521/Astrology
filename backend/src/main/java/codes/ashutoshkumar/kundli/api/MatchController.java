package codes.ashutoshkumar.kundli.api;

import codes.ashutoshkumar.kundli.ashtakoot.AshtakootResult;
import codes.ashutoshkumar.kundli.config.KundliProperties;
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
     * A stored match, with the full engine result re-inflated from its JSON.
     * {@link #rulesVersion} is kept for backward compatibility with existing
     * clients; {@link #ruleMetadata} is the structured block the spec asks
     * every result to expose.
     */
    public record MatchResponse(
            long id,
            long boyChartId,
            long girlChartId,
            String boyLabel,
            String girlLabel,
            AshtakootResult result,
            String rulesVersion,
            RuleMetadata ruleMetadata,
            String createdAt
    ) {}

    private final MatchService service;
    private final ObjectMapper mapper;
    private final KundliProperties props;

    public MatchController(MatchService service, ObjectMapper mapper, KundliProperties props) {
        this.service = service;
        this.mapper = mapper;
        this.props = props;
    }

    /** Current declared rule system — attached to results computed under it. */
    private RuleMetadata currentMetadata() {
        return RuleMetadata.from(props.rules());
    }

    /**
     * Metadata for a stored result. The historical {@code ashtakootaRuleVersion}
     * comes from the frozen {@link MatchRecord#getRulesVersion()} so a historical
     * match keeps its ash-koot version even after a config bump. Other fields
     * (ayanamsa, ephemeris mode) are not yet stored per-record; until Milestone 3
     * ships {@code ashtakoota-v1.0} and freezes the full block on the entity, we
     * report the current values for those. Fine while the whole block is one set.
     */
    private RuleMetadata metadataFor(MatchRecord r) {
        KundliProperties.Rules current = props.rules();
        return new RuleMetadata(
                current.ayanamsa(), current.matchingSystem(),
                r.getRulesVersion(), current.manglikRuleVersion(), current.ephemerisMode());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse create(@jakarta.validation.Valid @RequestBody CreateMatchRequest req) {
        MatchService.MatchOutcome outcome = service.match(req.boyChartId(), req.girlChartId());
        MatchRecord r = outcome.record();
        return new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                outcome.boy().getLabel(), outcome.girl().getLabel(),
                outcome.result(), r.getRulesVersion(), currentMetadata(), r.getCreatedAt());
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable long id) {
        MatchRecord r = service.get(id);
        return new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                null, null, parse(r.getResultJson()), r.getRulesVersion(),
                metadataFor(r), r.getCreatedAt());
    }

    @GetMapping
    public List<MatchResponse> list() {
        return service.list().stream()
                .map(r -> new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                        null, null, parse(r.getResultJson()), r.getRulesVersion(),
                        metadataFor(r), r.getCreatedAt()))
                .toList();
    }

    private AshtakootResult parse(String json) {
        try {
            return mapper.readValue(json, AshtakootResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored match result is unreadable", e);
        }
    }
}
