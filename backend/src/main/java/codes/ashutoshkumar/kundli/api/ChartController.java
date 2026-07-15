package codes.ashutoshkumar.kundli.api;

import codes.ashutoshkumar.kundli.api.ChartDtos.ChartResponse;
import codes.ashutoshkumar.kundli.api.ChartDtos.CreateChartRequest;
import codes.ashutoshkumar.kundli.attributes.KundliAttributesService;
import codes.ashutoshkumar.kundli.chart.BirthChart;
import codes.ashutoshkumar.kundli.chart.BirthChartService;
import codes.ashutoshkumar.kundli.chart.BirthChartService.CreateChartCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/charts")
public class ChartController {

    private final BirthChartService service;
    private final KundliAttributesService attributes;

    public ChartController(BirthChartService service, KundliAttributesService attributes) {
        this.service = service;
        this.attributes = attributes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChartResponse create(@Valid @RequestBody CreateChartRequest req) {
        var chart = service.create(new CreateChartCommand(
                req.label(), req.birthLocalDateTime(), req.timezone(),
                req.latitude(), req.longitude(), req.placeName()));
        return enrich(chart);
    }

    @GetMapping
    public List<ChartResponse> list() {
        return service.list().stream().map(this::enrich).toList();
    }

    @GetMapping("/{id}")
    public ChartResponse get(@PathVariable long id) {
        return enrich(service.get(id));
    }

    /** Attach derived kundli attributes (Gana, Nadi, Yoni, 7th house, …) to the response. */
    private ChartResponse enrich(BirthChart chart) {
        return ChartResponse.from(chart, attributes.derive(chart));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
