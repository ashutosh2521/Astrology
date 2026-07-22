package codes.ashutoshkumar.kundli.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;
import codes.ashutoshkumar.kundli.attributes.KundliAttributes.SeventhHouse;
import codes.ashutoshkumar.kundli.chart.BirthChart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Pins the read-only attribute derivation: the Moon-based koota attributes must agree
 * with the shared reference tables, and the whole-sign 7th-house math + benefic/malefic
 * verdict must be stable and transparent.
 */
class KundliAttributesServiceTest {

    private final KundliAttributesService service =
            new KundliAttributesService(ReferenceTables.load(), new ObjectMapper());

    /**
     * Build a chart. Moon in Ashwini (Nakshatra 1) / Mesha (Rashi 1); Lagna set by
     * {@code ascendantLongitude}; {@code grahaJson} is the grahas array body that
     * drives 7th-house occupancy.
     */
    private static BirthChart chart(Double ascendantLongitude, String grahaJson) {
        String chartJson = "{\"grahas\":[" + grahaJson + "]}";
        return new BirthChart(
                "Test", "1990-05-15T14:53:00", "Asia/Kolkata",
                25.6, 85.1, "Patna", "1990-05-15T09:23:00Z",
                1, 1, 1, 5.0, 5.0,
                ascendantLongitude, null, null,
                null, "LAHIRI", "SWISS_EPHEMERIS_FULL", chartJson,
                "2026-07-14T00:00:00Z");
    }

    @Test
    void moonAttributesComeFromTheReferenceTables() {
        // Moon in Ashwini / Mesha — values pinned from the verified reference JSON.
        KundliAttributes a = service.derive(chart(15.0, ""));
        assertEquals("DEVA", a.gana());
        assertEquals("AADI", a.nadi());
        assertEquals("HORSE", a.yoni());
        assertEquals("KSHATRIYA", a.varna());   // Mesha
        assertEquals("CHATUSHPADA", a.vashya()); // Mesha
        assertEquals("MARS", a.moonSignLord());  // Mesha lord
    }

    @Test
    void seventhHouseIsWholeSignFromLagnaWithItsLord() {
        // Ascendant 15° → Mesha Lagna → 7th sign is Tula, lord Venus.
        SeventhHouse h = service.derive(chart(15.0, "")).seventhHouse();
        assertEquals("Mesha", h.lagnaSign());
        assertEquals("Tula", h.sign());
        assertEquals("VENUS", h.lord());
        assertTrue(h.occupants().isEmpty());
        assertEquals("FAVOURABLE", h.assessment()); // empty 7th — nothing afflicting it
    }

    @Test
    void loneMaleficInSeventhNeedsAttention() {
        // Saturn (natural malefic) alone in Tula (sign 7).
        SeventhHouse h = service.derive(
                chart(15.0, "{\"name\":\"Saturn\",\"rashi\":{\"number\":7}}")).seventhHouse();
        assertEquals(1, h.occupants().size());
        assertEquals("Saturn", h.occupants().get(0).graha());
        assertEquals(false, h.occupants().get(0).benefic());
        assertEquals("NEEDS_ATTENTION", h.assessment());
    }

    @Test
    void beneficWithMaleficInSeventhIsMixed() {
        // Saturn (malefic) + Venus (benefic) both in the 7th → softened to MIXED.
        SeventhHouse h = service.derive(chart(15.0,
                "{\"name\":\"Saturn\",\"rashi\":{\"number\":7}},"
                        + "{\"name\":\"Venus\",\"rashi\":{\"number\":7}}")).seventhHouse();
        assertEquals(2, h.occupants().size());
        assertEquals("MIXED", h.assessment());
    }

    @Test
    void loneBeneficInSeventhIsFavourable() {
        SeventhHouse h = service.derive(
                chart(15.0, "{\"name\":\"Jupiter\",\"rashi\":{\"number\":7}}")).seventhHouse();
        assertEquals("FAVOURABLE", h.assessment());
        assertTrue(h.occupants().get(0).benefic());
    }

    @Test
    void legacyChartFallsBackToAscendantInChartJson() {
        // ascendantLongitude column is null (pre-Manglik chart) but chartJson has the ascendant.
        // The service must derive the 7th house from the blob rather than returning null.
        BirthChart legacy = new BirthChart(
                "Legacy", "1985-03-10T08:30:00", "Asia/Kolkata",
                25.6, 85.1, "Patna", "1985-03-10T03:00:00Z",
                1, 1, 1, 5.0, 5.0,
                null,   // <-- ascendantLongitude column absent
                null, null, null, "LAHIRI", "SWISS_EPHEMERIS_FULL",
                // chartJson always had the ascendant — 15° = Mesha lagna → 7th is Tula
                "{\"ascendant\":{\"longitude\":15.0},\"grahas\":[]}",
                "2024-01-01T00:00:00Z");

        SeventhHouse h = service.derive(legacy).seventhHouse();
        assertNotNull(h, "7th house must be derived from chartJson when column is null");
        assertEquals("Mesha", h.lagnaSign());
        assertEquals("Tula", h.sign());
    }

    @Test
    void legacyChartWithTrulyMissingAscendantHasNoSeventhHouse() {
        // Neither the column nor the blob carries an ascendant — nothing to show.
        BirthChart noAscendant = new BirthChart(
                "Old", "1985-03-10T08:30:00", "Asia/Kolkata",
                25.6, 85.1, null, "1985-03-10T03:00:00Z",
                1, 1, 1, 5.0, 5.0,
                null, null, null, null, "LAHIRI", "SWISS_EPHEMERIS_FULL",
                "{\"grahas\":[]}", "2024-01-01T00:00:00Z");

        assertNull(service.derive(noAscendant).seventhHouse());
    }
}
