package codes.ashutoshkumar.kundli.ashtakoot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the display-name contract between the backend and the Angular
 * frontend's i18n lookups.
 *
 * <p>The frontend keys its Hindi tables by title-case Sanskrit strings
 * ("Mesha", "Purva Ashadha"). Regressing to the ALL-CAPS enum {@link
 * Enum#name()} form ("MESHA", "PURVA_ASHADHA") silently breaks every
 * chart label in both Hindi AND English (the fallback returns the raw
 * input). This test is the guard.
 */
class DisplayNamesTest {

    @Test
    void everyRashiHasANonEmptyTitleCaseSanskritName() {
        for (Rashi r : Rashi.values()) {
            String title = r.sanskritTitle();
            assertNotNull(title, r + " missing sanskritTitle");
            assertFalse(title.isBlank(), r + " has blank sanskritTitle");
            // The whole point: NOT the ALL-CAPS enum identifier.
            assertFalse(title.equals(r.name()),
                    r + " sanskritTitle must not equal ALL-CAPS enum name");
            assertTrue(Character.isUpperCase(title.charAt(0)),
                    r + " sanskritTitle must start upper-case: " + title);
            assertFalse(title.contains("_"),
                    r + " sanskritTitle must not contain underscore: " + title);
        }
    }

    @Test
    void rashiSanskritTitleSpotChecks() {
        // These strings are also the keys in web/src/app/core/i18n.service.ts.
        // A change to either side that breaks agreement must fail this test.
        assertEquals("Mesha", Rashi.MESHA.sanskritTitle());
        assertEquals("Vrishabha", Rashi.VRISHABHA.sanskritTitle());
        assertEquals("Vrishchika", Rashi.VRISHCHIKA.sanskritTitle());
        assertEquals("Meena", Rashi.MEENA.sanskritTitle());
    }

    @Test
    void everyNakshatraHasANonEmptyTitleCaseSanskritName() {
        for (Nakshatra n : Nakshatra.values()) {
            String title = n.sanskritTitle();
            assertNotNull(title, n + " missing sanskritTitle");
            assertFalse(title.isBlank(), n + " has blank sanskritTitle");
            assertFalse(title.equals(n.name()),
                    n + " sanskritTitle must not equal ALL-CAPS enum name");
            assertTrue(Character.isUpperCase(title.charAt(0)),
                    n + " sanskritTitle must start upper-case: " + title);
            assertFalse(title.contains("_"),
                    n + " sanskritTitle must not contain underscore: " + title);
        }
    }

    @Test
    void multiWordNakshatrasUseSingleSpaceSeparator() {
        // Frontend Hindi keys use a single space, not an underscore:
        //   NAKSHATRA_HI['Purva Phalguni'], not 'PURVA_PHALGUNI'.
        assertEquals("Purva Phalguni", Nakshatra.PURVA_PHALGUNI.sanskritTitle());
        assertEquals("Uttara Phalguni", Nakshatra.UTTARA_PHALGUNI.sanskritTitle());
        assertEquals("Purva Ashadha", Nakshatra.PURVA_ASHADHA.sanskritTitle());
        assertEquals("Uttara Ashadha", Nakshatra.UTTARA_ASHADHA.sanskritTitle());
        assertEquals("Purva Bhadrapada", Nakshatra.PURVA_BHADRAPADA.sanskritTitle());
        assertEquals("Uttara Bhadrapada", Nakshatra.UTTARA_BHADRAPADA.sanskritTitle());
    }

    @Test
    void nakshatraSanskritTitleSpotChecks() {
        assertEquals("Ashwini", Nakshatra.ASHWINI.sanskritTitle());
        assertEquals("Krittika", Nakshatra.KRITTIKA.sanskritTitle());
        assertEquals("Revati", Nakshatra.REVATI.sanskritTitle());
    }
}
