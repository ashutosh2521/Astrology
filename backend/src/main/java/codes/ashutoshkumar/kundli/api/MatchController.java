package codes.ashutoshkumar.kundli.api;

import codes.ashutoshkumar.kundli.ashtakoot.AshtakootResult;
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

    /** A stored match, with the full engine result re-inflated from its JSON. */
    public record MatchResponse(
            long id,
            long boyChartId,
            long girlChartId,
            String boyLabel,
            String girlLabel,
            AshtakootResult result,
            String rulesVersion,
            String createdAt
    ) {}

    private final MatchService service;
    private final ObjectMapper mapper;

    public MatchController(MatchService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse create(@jakarta.validation.Valid @RequestBody CreateMatchRequest req) {
        MatchService.MatchOutcome outcome = service.match(req.boyChartId(), req.girlChartId());
        MatchRecord r = outcome.record();
        return new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                outcome.boy().getLabel(), outcome.girl().getLabel(),
                outcome.result(), r.getRulesVersion(), r.getCreatedAt());
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable long id) {
        MatchRecord r = service.get(id);
        return new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                null, null, parse(r.getResultJson()), r.getRulesVersion(), r.getCreatedAt());
    }

    @GetMapping
    public List<MatchResponse> list() {
        return service.list().stream()
                .map(r -> new MatchResponse(r.getId(), r.getBoyChartId(), r.getGirlChartId(),
                        null, null, parse(r.getResultJson()), r.getRulesVersion(), r.getCreatedAt()))
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
