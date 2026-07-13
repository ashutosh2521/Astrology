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
            new KundliProperties.Ephemeris("http://127.0.0.1:8001"), 1.0, 1950);

    private static ComputedChart chart(double toRashiBoundary, double toNakshatraBoundary) {
        return new ComputedChart(1, 1, 1, toRashiBoundary, toNakshatraBoundary,
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
}
