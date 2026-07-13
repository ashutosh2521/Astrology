package codes.ashutoshkumar.kundli.ashtakoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import codes.ashutoshkumar.kundli.ashtakoot.model.Gana;
import codes.ashutoshkumar.kundli.ashtakoot.model.Graha;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nadi;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import codes.ashutoshkumar.kundli.ashtakoot.model.Relation;
import codes.ashutoshkumar.kundli.ashtakoot.model.Varna;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;
import org.junit.jupiter.api.Test;

/** Table loading + completeness validation, and spot-checks of high-confidence mappings. */
class ReferenceTablesTest {

    private final ReferenceTables t = ReferenceTables.load();

    @Test
    void loadsAndValidatesWithoutError() {
        // load() runs strict completeness validation; reaching here means it passed.
        assertDoesNotThrow(ReferenceTables::load);
    }

    @Test
    void varnaElementalMapping() {
        assertEquals(Varna.BRAHMIN, t.varna(Rashi.KARKA));      // water
        assertEquals(Varna.KSHATRIYA, t.varna(Rashi.MESHA));    // fire
        assertEquals(Varna.VAISHYA, t.varna(Rashi.VRISHABHA));  // earth
        assertEquals(Varna.SHUDRA, t.varna(Rashi.MITHUNA));     // air
    }

    @Test
    void rashiLordsAreCorrect() {
        assertEquals(Graha.MARS, t.lord(Rashi.MESHA));
        assertEquals(Graha.MOON, t.lord(Rashi.KARKA));
        assertEquals(Graha.SUN, t.lord(Rashi.SIMHA));
        assertEquals(Graha.SATURN, t.lord(Rashi.KUMBHA));
        assertEquals(Graha.JUPITER, t.lord(Rashi.MEENA));
    }

    @Test
    void naturalFriendshipSample() {
        assertEquals(Relation.FRIEND, t.relation(Graha.SUN, Graha.MOON));
        assertEquals(Relation.ENEMY, t.relation(Graha.SUN, Graha.SATURN));
        assertEquals(Relation.ENEMY, t.relation(Graha.SATURN, Graha.SUN));
        assertEquals(Relation.ENEMY, t.relation(Graha.MERCURY, Graha.MOON));
    }

    @Test
    void ganaClassificationSample() {
        assertEquals(Gana.DEVA, t.gana(Nakshatra.ASHWINI));
        assertEquals(Gana.MANUSHYA, t.gana(Nakshatra.BHARANI));
        assertEquals(Gana.RAKSHASA, t.gana(Nakshatra.KRITTIKA));
    }

    @Test
    void nadiZigzagPattern() {
        assertEquals(Nadi.AADI, t.nadi(Nakshatra.ASHWINI));
        assertEquals(Nadi.MADHYA, t.nadi(Nakshatra.BHARANI));
        assertEquals(Nadi.ANTYA, t.nadi(Nakshatra.KRITTIKA));
        assertEquals(Nadi.ANTYA, t.nadi(Nakshatra.ROHINI));
        assertEquals(Nadi.MADHYA, t.nadi(Nakshatra.MRIGASHIRA));
        assertEquals(Nadi.AADI, t.nadi(Nakshatra.ARDRA));
    }
}
