package codes.ashutoshkumar.kundli.ashtakoot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ashutoshkumar.kundli.ashtakoot.AshtakootEngine;
import codes.ashutoshkumar.kundli.ashtakoot.AshtakootResult;
import codes.ashutoshkumar.kundli.ashtakoot.DoshaStatus;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Cross-checks engine output against an external reference (Drik Panchang), per the
 * spec's validation strategy. The reference values live in
 * {@code /regression/koota_reference_cases.json}.
 *
 * <p>Workflow: each case's inputs are pre-filled to exercise a specific koota or
 * cancellation path; a human fills {@code expected} from Drik Panchang and flips
 * {@code status} to {@code FILLED}. Until then the case is SKIPPED (not failed), so
 * the suite is honest about being incomplete rather than green-by-emptiness. Once
 * filled, a mismatch FAILS and pinpoints the wrong provisional table cell.
 */
class KootaRegressionTest {

    private static final String FILLED = "FILLED";
    private static final double TOL = 1e-9;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChartRef(String rashi, String nakshatra, int pada) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Expected(Double varna, Double vashya, Double tara, Double yoni,
                    Double grahaMaitri, Double gana, Double bhakoot, Double nadi,
                    Double total, Boolean nadiDoshaEffective, Boolean bhakootDoshaEffective) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Case(String id, String note, ChartRef boy, ChartRef girl, String status, Expected expected) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CaseFile(java.util.List<Case> cases) {}

    private static CaseFile load() throws Exception {
        ObjectMapper om = new ObjectMapper().configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = KootaRegressionTest.class.getResourceAsStream(
                "/regression/koota_reference_cases.json")) {
            assertNotNull(in, "regression fixture not found on classpath");
            return om.readValue(in, CaseFile.class);
        }
    }

    private static MoonChart toChart(ChartRef c) {
        return new MoonChart(Rashi.valueOf(c.rashi()), Nakshatra.valueOf(c.nakshatra()), c.pada());
    }

    /** One dynamic test per reference case. Pending cases are skipped; filled cases assert. */
    @TestFactory
    Stream<DynamicTest> referenceCases() throws Exception {
        AshtakootEngine engine = AshtakootEngine.create();
        return load().cases().stream()
                .map(c -> DynamicTest.dynamicTest(c.id(), () -> runCase(engine, c)));
    }

    private void runCase(AshtakootEngine engine, Case c) {
        Assumptions.assumeTrue(FILLED.equals(c.status()), () ->
                "PENDING: fill 'expected' for '" + c.id() + "' from Drik Panchang, then set status FILLED");
        Expected e = c.expected();
        assertNotNull(e, "case " + c.id() + " is FILLED but has no expected block");

        AshtakootResult r = engine.match(toChart(c.boy()), toChart(c.girl()));
        Map<String, Double> got = r.kootas().stream()
                .collect(Collectors.toMap(KootaScore::koota, KootaScore::points));

        check(c.id(), "Varna", e.varna(), got.get("Varna"));
        check(c.id(), "Vashya", e.vashya(), got.get("Vashya"));
        check(c.id(), "Tara", e.tara(), got.get("Tara"));
        check(c.id(), "Yoni", e.yoni(), got.get("Yoni"));
        check(c.id(), "Graha Maitri", e.grahaMaitri(), got.get("Graha Maitri"));
        check(c.id(), "Gana", e.gana(), got.get("Gana"));
        check(c.id(), "Bhakoot", e.bhakoot(), got.get("Bhakoot"));
        check(c.id(), "Nadi", e.nadi(), got.get("Nadi"));

        if (e.total() != null) {
            assertEquals(e.total(), r.totalPoints(), TOL, "[" + c.id() + "] total mismatch");
        }
        if (e.nadiDoshaEffective() != null) {
            assertEquals(e.nadiDoshaEffective(), effective(r, "Nadi"),
                    "[" + c.id() + "] Nadi dosha effective mismatch");
        }
        if (e.bhakootDoshaEffective() != null) {
            assertEquals(e.bhakootDoshaEffective(), effective(r, "Bhakoot"),
                    "[" + c.id() + "] Bhakoot dosha effective mismatch");
        }
    }

    private static void check(String id, String koota, Double expected, Double got) {
        if (expected == null) {
            return; // field intentionally omitted — assert only what the fixture provides
        }
        assertNotNull(got, "[" + id + "] engine produced no score for " + koota);
        assertEquals(expected, got, TOL, "[" + id + "] " + koota + " mismatch");
    }

    private static boolean effective(AshtakootResult r, String name) {
        return r.doshas().stream()
                .filter(d -> d.name().equals(name))
                .map(DoshaStatus::isEffective)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Guards fixture quality now (runs green today): every case must reference valid
     * enums, a pada in 1..4, and a unique id. This keeps the reference file from silently
     * rotting even while cases are still pending Drik Panchang values.
     */
    @Test
    void fixtureIsWellFormed() throws Exception {
        CaseFile file = load();
        assertTrue(file.cases().size() >= 1, "expected at least one reference case");
        Set<String> ids = new HashSet<>();
        for (Case c : file.cases()) {
            assertTrue(ids.add(c.id()), "duplicate case id: " + c.id());
            // Throws if a Rashi/Nakshatra name is invalid or pada is out of range.
            Function<ChartRef, MoonChart> build = KootaRegressionTest::toChart;
            assertNotNull(build.apply(c.boy()), c.id() + " boy chart");
            assertNotNull(build.apply(c.girl()), c.id() + " girl chart");
        }
    }
}
