package codes.ashutoshkumar.kundli.profile;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import codes.ashutoshkumar.kundli.api.ChartDtos;
import codes.ashutoshkumar.kundli.chart.EphemerisClient;
import codes.ashutoshkumar.kundli.chart.EphemerisClient.ComputedChart;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full-stack test of the Mother-mode primary profile:
 *   - GET returns configured=false on first run.
 *   - PUT with a real chart id sets the primary and returns configured=true
 *     with the full chart inflated.
 *   - GET after PUT reads back the same pointer.
 *   - PUT with an unknown chart id 404s (never persisted).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileEndpointTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ProfileRepository profileRepository;
    @MockBean private EphemerisClient ephemeris;

    /**
     * The test JVM is shared across @SpringBootTest classes, so the profile
     * table can carry over from another integration test. Reset to the
     * "first-run" state at the start of every test so ordering is irrelevant.
     */
    @BeforeEach
    void resetProfile() {
        profileRepository.deleteAll();
    }

    private static ComputedChart moon(int rashi, int nakshatra, int pada) {
        return new ComputedChart(rashi, nakshatra, pada, 15.0, 6.0,
                123.4, 45.6, 78.9,
                "SWISS_EPHEMERIS_FULL", "LAHIRI", "{\"stub\":true}");
    }

    private long createChart(String label) throws Exception {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble())).thenReturn(moon(1, 1, 1));
        String body = mapper.writeValueAsString(new ChartDtos.CreateChartRequest(
                label, "1998-10-25T16:20:00", "Asia/Kolkata", 23.3441, 85.3096,
                "Ranchi, Jharkhand, India"));
        MvcResult r = mvc.perform(post("/api/charts").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void firstRunProfileIsNotConfigured() throws Exception {
        mvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.primaryChartId").doesNotExist())
                .andExpect(jsonPath("$.primaryChart").doesNotExist());
    }

    @Test
    void putPointsPrimaryAtExistingChartAndInflatesIt() throws Exception {
        long chartId = createChart("Ashutosh Kumar");

        mvc.perform(put("/api/profile").contentType("application/json")
                        .content("{\"chartId\":%d}".formatted(chartId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.primaryChartId").value(chartId))
                .andExpect(jsonPath("$.primaryChart.id").value(chartId))
                .andExpect(jsonPath("$.primaryChart.label").value("Ashutosh Kumar"));

        // Round-trip: GET now returns the same pointer.
        mvc.perform(get("/api/profile"))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.primaryChartId").value(chartId));
    }

    @Test
    void putWithUnknownChartId404s() throws Exception {
        mvc.perform(put("/api/profile").contentType("application/json")
                        .content("{\"chartId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("999999")));
    }

    @Test
    void putIsIdempotentAndRepointable() throws Exception {
        long first = createChart("First");
        long second = createChart("Second");

        mvc.perform(put("/api/profile").contentType("application/json")
                        .content("{\"chartId\":%d}".formatted(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryChartId").value(first));

        // Repoint at a different chart — the same row is updated, not a new row.
        mvc.perform(put("/api/profile").contentType("application/json")
                        .content("{\"chartId\":%d}".formatted(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryChartId").value(second));

        mvc.perform(get("/api/profile"))
                .andExpect(jsonPath("$.primaryChartId").value(second));
    }
}
