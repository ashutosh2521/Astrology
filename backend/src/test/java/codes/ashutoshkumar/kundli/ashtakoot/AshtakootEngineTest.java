package codes.ashutoshkumar.kundli.ashtakoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import org.junit.jupiter.api.Test;

class AshtakootEngineTest {

    private final AshtakootEngine engine = AshtakootEngine.create();

    private static MoonChart chart(Rashi r, Nakshatra n, int pada) {
        return new MoonChart(r, n, pada);
    }

    private static DoshaStatus dosha(AshtakootResult result, String name) {
        return result.doshas().stream()
                .filter(d -> d.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void matchReturnsEightKootasAndTotalInRange() {
        AshtakootResult r = engine.match(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),
                chart(Rashi.VRISHABHA, Nakshatra.ROHINI, 2));
        assertEquals(8, r.kootas().size());
        assertEquals(36.0, r.maxPoints());
        assertTrue(r.totalPoints() >= 0.0 && r.totalPoints() <= 36.0);
        // Total must equal the sum of the koota points.
        double sum = r.kootas().stream().mapToDouble(KootaScore::points).sum();
        assertEquals(sum, r.totalPoints(), 1e-9);
    }

    @Test
    void nadiDoshaCancelledBySameNakshatraDifferentPada() {
        AshtakootResult r = engine.match(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 2));
        DoshaStatus nadi = dosha(r, "Nadi");
        assertTrue(nadi.present(), "same Nakshatra => same Nadi => dosha present");
        assertTrue(nadi.cancelled(), "different pada => cancelled by convention");
        assertFalse(nadi.isEffective());
    }

    @Test
    void bhakootDoshaCancelledBySameLord() {
        // Mesha & Vrishchika are 6/8 apart (Bhakoot dosha) but share lord Mars.
        AshtakootResult r = engine.match(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),
                chart(Rashi.VRISHCHIKA, Nakshatra.JYESHTHA, 1));
        DoshaStatus bhakoot = dosha(r, "Bhakoot");
        assertTrue(bhakoot.present());
        assertTrue(bhakoot.cancelled(), "shared lord Mars cancels Bhakoot dosha");
        assertFalse(bhakoot.isEffective());
    }

    @Test
    void bhakootDoshaEffectiveWhenLordsAreEnemies() {
        // Mesha (Mars) & Kanya (Mercury) are 6/8 apart; Mars-Mercury are enemies → not cancelled.
        AshtakootResult r = engine.match(
                chart(Rashi.MESHA, Nakshatra.ASHWINI, 1),
                chart(Rashi.KANYA, Nakshatra.HASTA, 1));
        DoshaStatus bhakoot = dosha(r, "Bhakoot");
        assertTrue(bhakoot.present());
        assertFalse(bhakoot.cancelled());
        assertTrue(bhakoot.isEffective());
        assertTrue(r.verdict().contains("Bhakoot"), "verdict should flag the effective dosha");
    }

    @Test
    void identicalChartsProduceNadiDoshaThatIsNotCancelled() {
        // Exact same Nakshatra and Pada: same Nadi, and no pada/rashi difference to cancel it.
        MoonChart same = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        AshtakootResult r = engine.match(same, same);
        DoshaStatus nadi = dosha(r, "Nadi");
        assertTrue(nadi.present());
        assertFalse(nadi.cancelled());
        assertTrue(nadi.isEffective());
    }
}
