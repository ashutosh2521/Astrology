package codes.ashutoshkumar.kundli.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import codes.ashutoshkumar.kundli.chart.EphemerisClient.ComputedChart;
import codes.ashutoshkumar.kundli.config.KundliProperties;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the backend-owned accuracy policies: local→UTC resolution,
 * boundary-warning thresholding, and the historical-timezone warning.
 * Pure Mockito — no Spring context, no database, no ephemeris service.
 */
class BirthChartServiceTest {

    private BirthChartRepository repository;
    private EphemerisClient ephemeris;
    private BirthChartService service;

    private static final KundliProperties PROPS = new KundliProperties(
            new KundliProperties.Ephemeris("http://127.0.0.1:8001"), 1.0, 1950,
            new KundliProperties.Rules("LAHIRI", "NORTH_INDIAN_ASHTAKOOTA",
                    "ashtakoot-v0-provisional", "manglik-v1.1", "SWISS_EPHEMERIS_FULL"),
            new KundliProperties.Recommendation(18.0, 25.0));

    private static ComputedChart chart(double toRashiBoundary, double toNakshatraBoundary) {
        // Placeholder longitudes for Ascendant/Mars/Venus — the service under
        // test doesn't consume them; Milestone 4 (Manglik) will.
        return new ComputedChart(1, 1, 1, toRashiBoundary, toNakshatraBoundary,
                123.4, 45.6, 78.9,
                "SWISS_EPHEMERIS_FULL", "LAHIRI", "{}");
    }

    @BeforeEach
    void setUp() {
        repository = mock(BirthChartRepository.class);
        ephemeris = mock(EphemerisClient.class);
        service = new BirthChartService(repository, ephemeris, PROPS);
        // save() returns its argument so we can assert on the entity that was built.
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private BirthChart create(String localDateTime, String tz) {
        return service.create(new BirthChartService.CreateChartCommand(
                "Test", localDateTime, tz, 28.6139, 77.2090, "New Delhi"));
    }

    @Test
    void resolvesKolkataLocalTimeToUtc() {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(chart(15.0, 6.0));
        BirthChart c = create("1990-05-15T14:53:00", "Asia/Kolkata");
        // IST is UTC+5:30 → 14:53 local = 09:23 UTC.
        assertEquals("1990-05-15T09:23:00Z", c.getUtcInstant());
        assertNull(c.getWarnings(), "no warnings expected for a modern, non-boundary birth");
    }

    @Test
    void utcInstantIsWhatTheEphemerisReceives() {
        when(ephemeris.computeChart(eq(Instant.parse("1990-05-15T09:23:00Z")),
                anyDouble(), anyDouble())).thenReturn(chart(15.0, 6.0));
        create("1990-05-15T14:53:00", "Asia/Kolkata");
        // Strict eq() above: the mock would throw if the service sent any other instant.
    }

    @Test
    void nakshatraBoundaryWithinThresholdWarns() {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(chart(15.0, 0.4));
        BirthChart c = create("1990-05-15T14:53:00", "Asia/Kolkata");
        assertTrue(c.getWarnings().contains("Nakshatra boundary"));
    }

    @Test
    void rashiBoundaryWithinThresholdWarns() {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(chart(0.7, 6.0));
        BirthChart c = create("1990-05-15T14:53:00", "Asia/Kolkata");
        assertTrue(c.getWarnings().contains("Rashi boundary"));
    }

    @Test
    void pre1950BirthGetsHistoricalTimezoneWarning() {
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(chart(15.0, 6.0));
        BirthChart c = create("1942-03-10T04:30:00", "Asia/Kolkata");
        assertTrue(c.getWarnings().contains("historical"));
    }

    @Test
    void invalidTimezoneRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> create("1990-05-15T14:53:00", "Asia/NotAPlace"));
    }

    @Test
    void invalidDateTimeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> create("15/05/1990 2:53 PM", "Asia/Kolkata"));
    }

    // -----------------------------------------------------------------
    // Ashutosh Kumar canonical profile (docs/MILESTONE1_AUDIT.md §13.2).
    //
    // Birth: 25 Oct 1998, 04:20 PM IST, Ranchi, Jharkhand.
    // In 24-hour form the birth time is 16:20:00; IST is UTC+5:30, so
    // the UTC instant that reaches the ephemeris service must be exactly
    // 1998-10-25T10:50:00Z. This app is built for this profile — a
    // silent 04:20 AM / 04:20 PM confusion would produce a completely
    // wrong Moon, Ascendant and match result.
    // -----------------------------------------------------------------

    private static final double RANCHI_LAT = 23.3441;
    private static final double RANCHI_LON = 85.3096;

    private BirthChart createAshutosh(String localDateTime) {
        return service.create(new BirthChartService.CreateChartCommand(
                "Ashutosh Kumar", localDateTime, "Asia/Kolkata",
                RANCHI_LAT, RANCHI_LON, "Ranchi, Jharkhand, India"));
    }

    @Test
    void ashutosh1620PmIstResolvesTo1050UtcSameDate() {
        when(ephemeris.computeChart(eq(Instant.parse("1998-10-25T10:50:00Z")),
                eq(RANCHI_LAT), eq(RANCHI_LON))).thenReturn(chart(15.0, 6.0));
        BirthChart c = createAshutosh("1998-10-25T16:20:00");
        // Strict eq() above means the mock threw if the service sent any
        // other instant. The stored UTC round-trips the same value; that
        // is the invariant the primary profile relies on.
        assertEquals("1998-10-25T10:50:00Z", c.getUtcInstant());
        assertEquals("Asia/Kolkata", c.getTimezone());
    }

    @Test
    void ashutosh0420AmMustNotBeAliasedTo1620Pm() {
        // Whichever branch of code reads local time, an accidental "PM
        // means AM" (or vice versa) must never fold these two into the
        // same UTC. This test locks that in.
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(chart(15.0, 6.0));

        BirthChart pm = createAshutosh("1998-10-25T16:20:00");
        BirthChart am = createAshutosh("1998-10-25T04:20:00");

        // 12 hours apart in local time → 12 hours apart in UTC.
        Instant pmUtc = Instant.parse(pm.getUtcInstant());
        Instant amUtc = Instant.parse(am.getUtcInstant());
        assertEquals(12 * 3600L, pmUtc.getEpochSecond() - amUtc.getEpochSecond(),
                "04:20 must not be silently coerced into 16:20");

        // Specifically: 04:20 IST is the PREVIOUS UTC day.
        assertEquals("1998-10-24T22:50:00Z", am.getUtcInstant(),
                "04:20 IST resolves to the previous UTC day, not the same day");
    }

    @Test
    void ashutosh1620PmProducesNoHistoricalTimezoneWarning() {
        // 1998 is well past the historicalTzWarningBeforeYear (1950); IST
        // has been stable at UTC+5:30 since 1945. No warning expected.
        when(ephemeris.computeChart(any(), anyDouble(), anyDouble()))
                .thenReturn(chart(15.0, 6.0));
        BirthChart c = createAshutosh("1998-10-25T16:20:00");
        assertNull(c.getWarnings(),
                "Ashutosh's 1998 birth is post-1950 and non-boundary; no warnings expected");
    }
}
