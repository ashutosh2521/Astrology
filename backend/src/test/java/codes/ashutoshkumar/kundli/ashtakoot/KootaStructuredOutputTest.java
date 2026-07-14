package codes.ashutoshkumar.kundli.ashtakoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ashutoshkumar.kundli.ashtakoot.koota.BhakootKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.GanaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.GrahaMaitriKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.NadiKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.TaraKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.VarnaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.VashyaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.YoniKoota;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;
import org.junit.jupiter.api.Test;

/**
 * Pins the structured koota-output shape from Milestone 3.1 (per the brief's
 * example and audit §11): every koota emits a stable {@link KootaCode} plus
 * title-case Sanskrit values for both partners plus a {@code ruleApplied}
 * code. Translations are the frontend's job — the engine never emits Hindi.
 */
class KootaStructuredOutputTest {

    private final ReferenceTables t = ReferenceTables.load();

    private static MoonChart chart(Rashi r, Nakshatra n, int pada) {
        return new MoonChart(r, n, pada);
    }

    private static void assertBasicShape(KootaScore s, KootaCode expectedCode) {
        assertEquals(expectedCode, s.code(), "code must match the koota");
        assertNotNull(s.koota(), "legacy display name still populated");
        assertNotNull(s.personAValue(), "personAValue populated");
        assertNotNull(s.personBValue(), "personBValue populated");
        assertNotNull(s.ruleApplied(), "ruleApplied populated");
        assertFalse(s.ruleApplied().isBlank(), "ruleApplied not blank");
        assertTrue(s.points() >= 0 && s.points() <= s.maxPoints(),
                "points within [0, max] for " + s.koota());
    }

    @Test
    void varnaEmitsTitleCaseTiersAndBranchCode() {
        KootaScore s = new VarnaKoota().score(
                chart(Rashi.KARKA, Nakshatra.PUSHYA, 1),      // Brahmin
                chart(Rashi.MITHUNA, Nakshatra.ARDRA, 1), t); // Shudra
        assertBasicShape(s, KootaCode.VARNA);
        assertEquals("Brahmin", s.personAValue());
        assertEquals("Shudra", s.personBValue());
        assertEquals(VarnaKoota.RULE_BOY_TIER_GTE_GIRL, s.ruleApplied());
        assertFalse(s.doshaPresent());
    }

    @Test
    void vashyaCarriesGroupTitlesAndSameGroupCodeOnMatch() {
        KootaScore s = new VashyaKoota().score(
                chart(Rashi.MITHUNA, Nakshatra.ARDRA, 1),   // MANAVA
                chart(Rashi.KANYA, Nakshatra.HASTA, 1), t); // MANAVA
        assertBasicShape(s, KootaCode.VASHYA);
        assertEquals("Manava", s.personAValue());
        assertEquals("Manava", s.personBValue());
        assertEquals(VashyaKoota.RULE_SAME_VASHYA_GROUP, s.ruleApplied());
    }

    @Test
    void taraExposesMod9RemaindersInPersonValues() {
        // boy Ashwini, girl Krittika: count 3 (Vipat, bad) forward; 26 mod9 = 8 (good) reverse.
        KootaScore s = new TaraKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),
                chart(Rashi.MESHA, Nakshatra.KRITTIKA, 1), t);
        assertBasicShape(s, KootaCode.TARA);
        assertEquals("3", s.personAValue(), "boy→girl mod-9 remainder in personAValue");
        assertEquals("8", s.personBValue(), "girl→boy mod-9 remainder in personBValue");
        assertEquals(TaraKoota.RULE_ONE_DIRECTION_AUSPICIOUS, s.ruleApplied());
        assertEquals(1.5, s.points(), 1e-9);
    }

    @Test
    void yoniSameAnimalUsesSameYoniCode() {
        KootaScore s = new YoniKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),     // Horse
                chart(Rashi.KUMBHA, Nakshatra.SHATABHISHA, 1), t); // Horse
        assertBasicShape(s, KootaCode.YONI);
        assertEquals("Horse", s.personAValue());
        assertEquals("Horse", s.personBValue());
        assertEquals(YoniKoota.RULE_SAME_YONI, s.ruleApplied());
        assertEquals(4.0, s.points(), 1e-9);
    }

    @Test
    void yoniSwornEnemyUsesEnemyCode() {
        KootaScore s = new YoniKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),   // Horse
                chart(Rashi.KANYA, Nakshatra.HASTA, 1), t); // Buffalo
        assertBasicShape(s, KootaCode.YONI);
        assertEquals(YoniKoota.RULE_SWORN_ENEMY, s.ruleApplied());
        assertEquals(0.0, s.points(), 1e-9);
    }

    @Test
    void grahaMaitriSameLordUsesSameLordCode() {
        KootaScore s = new GrahaMaitriKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),        // Mars
                chart(Rashi.VRISHCHIKA, Nakshatra.JYESHTHA, 1), t); // Mars
        assertBasicShape(s, KootaCode.GRAHA_MAITRI);
        assertEquals("Mars", s.personAValue());
        assertEquals("Mars", s.personBValue());
        assertEquals(GrahaMaitriKoota.RULE_SAME_MOON_SIGN_LORD, s.ruleApplied());
        assertEquals(5.0, s.points(), 1e-9);
    }

    @Test
    void grahaMaitriBandCodeCarriesOrderIndependentKey() {
        // Sun-Saturn: mutual enemies (ENEMY, ENEMY) → BAND_ENEMY_ENEMY
        KootaScore s = new GrahaMaitriKoota().score(
                chart(Rashi.SIMHA, Nakshatra.MAGHA, 1),        // Sun
                chart(Rashi.MAKARA, Nakshatra.SHRAVANA, 1), t); // Saturn
        assertBasicShape(s, KootaCode.GRAHA_MAITRI);
        assertEquals("Sun", s.personAValue());
        assertEquals("Saturn", s.personBValue());
        assertEquals(GrahaMaitriKoota.RULE_BAND_PREFIX + "ENEMY_ENEMY", s.ruleApplied());
    }

    @Test
    void ganaSameGanaUsesSameCode() {
        KootaScore s = new GanaKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),           // Deva
                chart(Rashi.VRISHABHA, Nakshatra.MRIGASHIRA, 4), t); // Deva
        assertBasicShape(s, KootaCode.GANA);
        assertEquals("Deva", s.personAValue());
        assertEquals("Deva", s.personBValue());
        assertEquals(GanaKoota.RULE_SAME_GANA, s.ruleApplied());
    }

    @Test
    void bhakootDoshaPairCarriesTheSpecificPairCode() {
        // Mesha / Vrishabha = 2-12 → Bhakoot dosha, specific pair.
        KootaScore s = new BhakootKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),
                chart(Rashi.VRISHABHA, Nakshatra.KRITTIKA, 3), t);
        assertBasicShape(s, KootaCode.BHAKOOT);
        assertEquals(BhakootKoota.RULE_DOSHA_2_12, s.ruleApplied());
        assertTrue(s.doshaPresent(), "Bhakoot dosha pair sets doshaPresent");
        assertEquals(0.0, s.points(), 1e-9);
    }

    @Test
    void bhakootNoDoshaUsesNoDoshaCode() {
        // Mesha / Tula = 7-7 → no dosha (7 not in {2/12, 5/9, 6/8}).
        KootaScore s = new BhakootKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),
                chart(Rashi.TULA, Nakshatra.SWATI, 1), t);
        assertBasicShape(s, KootaCode.BHAKOOT);
        assertEquals(BhakootKoota.RULE_NO_DOSHA, s.ruleApplied());
        assertFalse(s.doshaPresent());
        assertEquals(7.0, s.points(), 1e-9);
    }

    @Test
    void nadiSameNadiSetsDoshaAndSpecificCode() {
        // Same Nakshatra + Pada: same Nadi, dosha present, not cancelled.
        MoonChart same = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        KootaScore s = new NadiKoota().score(same, same, t);
        assertBasicShape(s, KootaCode.NADI);
        assertEquals("Aadi", s.personAValue());
        assertEquals("Aadi", s.personBValue());
        assertEquals(NadiKoota.RULE_SAME_NADI_DOSHA, s.ruleApplied());
        assertTrue(s.doshaPresent());
        assertEquals(0.0, s.points(), 1e-9);
    }

    @Test
    void nadiDifferentNadiUsesFullScoreCode() {
        KootaScore s = new NadiKoota().score(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),   // Aadi
                chart(Rashi.MESHA, Nakshatra.BHARANI, 1), t); // Madhya
        assertBasicShape(s, KootaCode.NADI);
        assertEquals("Aadi", s.personAValue());
        assertEquals("Madhya", s.personBValue());
        assertEquals(NadiKoota.RULE_DIFFERENT_NADI_FULL, s.ruleApplied());
        assertFalse(s.doshaPresent());
        assertEquals(8.0, s.points(), 1e-9);
    }
}
