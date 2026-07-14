package codes.ashutoshkumar.kundli.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import codes.ashutoshkumar.kundli.chart.EphemerisClient;
import codes.ashutoshkumar.kundli.chart.EphemerisClient.ComputedChart;
import codes.ashutoshkumar.kundli.chart.EphemerisUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full-stack integration: real Spring context, real SQLite (throwaway file under
 * target/), real Ashtakoot engine — only the ephemeris HTTP call is mocked, since
 * the Python service isn't running in this environment.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EphemerisClient ephemeris;

    private static ComputedChart moon(int rashi, int nakshatra, int pada) {
        return new ComputedChart(rashi, nakshatra, pada, 15.0, 6.0,
                123.4, 45.6, 78.9,
                "SWISS_EPHEMERIS_FULL", "LAHIRI", "{\"stub\":true}");
    }

    private long createChart(String label, int rashi, int nakshatra, int pada) throws Exception {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(moon(rashi, nakshatra, pada));
        String body = mapper.writeValueAsString(new ChartDtos.CreateChartRequest(
                label, "1990-05-15T14:53:00", "Asia/Kolkata", 28.6139, 77.2090, "New Delhi"));
        MvcResult result = mvc.perform(post("/api/charts")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precision").value("SWISS_EPHEMERIS_FULL"))
                .andReturn();
        return ((Number) JsonPath.read(
                result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void chartAndMatchEndToEnd() throws Exception {
        // Boy: Mesha/Ashwini-1; girl: Mesha/Ashwini-2 → Nadi dosha present but
        // cancelled (same Nakshatra, different Pada) — exercises the cancellation path
        // through the whole stack, not just the engine.
        long boy = createChart("Boy", 1, 1, 1);
        long girl = createChart("Girl", 1, 1, 2);

        String matchBody = "{\"boyChartId\":%d,\"girlChartId\":%d}".formatted(boy, girl);
        MvcResult match = mvc.perform(post("/api/matches")
                        .contentType("application/json").content(matchBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.kootas", hasSize(8)))
                .andExpect(jsonPath("$.result.doshas[0].name").value("Nadi"))
                .andExpect(jsonPath("$.result.doshas[0].present").value(true))
                .andExpect(jsonPath("$.result.doshas[0].cancelled").value(true))
                .andExpect(jsonPath("$.rulesVersion").value(containsString("provisional")))
                // Declared rule system block (spec: every result exposes metadata).
                .andExpect(jsonPath("$.ruleMetadata.ayanamsa").value("LAHIRI"))
                .andExpect(jsonPath("$.ruleMetadata.matchingSystem").value("NORTH_INDIAN_ASHTAKOOTA"))
                .andExpect(jsonPath("$.ruleMetadata.ashtakootaRuleVersion")
                        .value(containsString("provisional")))
                .andExpect(jsonPath("$.ruleMetadata.manglikRuleVersion").value("manglik-v1.0"))
                // Manglik result now populated on every new match — both partners
                // and the couple compatibility must appear on the wire.
                .andExpect(jsonPath("$.manglik.personA.status").exists())
                .andExpect(jsonPath("$.manglik.personB.status").exists())
                .andExpect(jsonPath("$.manglik.compatibility").exists())
                .andExpect(jsonPath("$.manglik.ruleVersion").value("manglik-v1.0"))
                .andExpect(jsonPath("$.ruleMetadata.ephemerisMode").value("SWISS_EPHEMERIS_FULL"))
                .andReturn();

        // The stored match reads back identically (SQLite round-trip of the result JSON).
        long matchId = ((Number) JsonPath.read(
                match.getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(get("/api/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.kootas", hasSize(8)))
                .andExpect(jsonPath("$.result.doshas[0].cancelled").value(true))
                // The rule-metadata block also appears on GET, and its
                // ashtakootaRuleVersion is the frozen historical value from the
                // stored MatchRecord — a later config bump must not silently
                // relabel historical results.
                .andExpect(jsonPath("$.ruleMetadata.ashtakootaRuleVersion")
                        .value(containsString("provisional")));
    }

    @Test
    void chartValidationRejectsBadTimezone() throws Exception {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(moon(1, 1, 1));
        String body = mapper.writeValueAsString(new ChartDtos.CreateChartRequest(
                "X", "1990-05-15T14:53:00", "Not/AZone", 28.6, 77.2, null));
        mvc.perform(post("/api/charts").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("IANA")));
    }

    @Test
    void matchAgainstMissingChartIs404() throws Exception {
        mvc.perform(post("/api/matches").contentType("application/json")
                        .content("{\"boyChartId\":999999,\"girlChartId\":999998}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ephemerisFailureSurfacesAs502NotSilentDegradation() throws Exception {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenThrow(new EphemerisUnavailableException("Moshier fallback detected"));
        String body = mapper.writeValueAsString(new ChartDtos.CreateChartRequest(
                "X", "1990-05-15T14:53:00", "Asia/Kolkata", 28.6, 77.2, null));
        mvc.perform(post("/api/charts").contentType("application/json").content(body))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error", containsString("Moshier")));
    }
}
