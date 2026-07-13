package codes.ashutoshkumar.kundli.ashtakoot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import codes.ashutoshkumar.kundli.ashtakoot.koota.BhakootKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.GanaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.GrahaMaitriKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.NadiKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.TaraKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.VarnaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.YoniKoota;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;
import org.junit.jupiter.api.Test;

/**
 * Per-koota unit tests. These pin the *rules* the engine must obey. Values for the
 * provisional matrices (Vashya, Yoni gradations, Gana, Graha Maitri bands) will be
 * re-pinned once cross-checked against Drik Panchang in the regression suite.
 */
class KootaScorersTest {

    private final ReferenceTables t = ReferenceTables.load();

    private static MoonChart chart(Rashi r, Nakshatra n, int pada) {
        return new MoonChart(r, n, pada);
    }

    // ---- Varna (high confidence) ----

    @Test
    void varnaBoyHigherOrEqualScores() {
        MoonChart brahminBoy = chart(Rashi.KARKA, Nakshatra.PUSHYA, 1);
        MoonChart shudraGirl = chart(Rashi.MITHUNA, Nakshatra.ARDRA, 1);
        assertEquals(1.0, new VarnaKoota().score(brahminBoy, shudraGirl, t).points());
    }

    @Test
    void varnaBoyLowerScoresZero() {
        MoonChart shudraBoy = chart(Rashi.MITHUNA, Nakshatra.ARDRA, 1);
        MoonChart brahminGirl = chart(Rashi.KARKA, Nakshatra.PUSHYA, 1);
        assertEquals(0.0, new VarnaKoota().score(shudraBoy, brahminGirl, t).points());
    }

    // ---- Tara (high confidence, algorithmic) ----

    @Test
    void taraBothDirectionsAuspicious() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        MoonChart girl = chart(Rashi.MESHA, Nakshatra.BHARANI, 1);
        // boy→girl count 2 (mod9=2, good); girl→boy count 27 (mod9=0, good) → 3.0
        assertEquals(3.0, new TaraKoota().score(boy, girl, t).points());
    }

    @Test
    void taraInauspiciousRemainderThree() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        MoonChart girl = chart(Rashi.MESHA, Nakshatra.KRITTIKA, 1);
        // boy→girl count 3 (Vipat, bad → 0); girl→boy count 26 (mod9=8, good → 1.5)
        assertEquals(1.5, new TaraKoota().score(boy, girl, t).points());
    }

    // ---- Bhakoot (dosha pairs high confidence) ----

    @Test
    void bhakootDoshaPairScoresZero() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        MoonChart girl = chart(Rashi.VRISHABHA, Nakshatra.KRITTIKA, 3); // 2/12
        assertEquals(0.0, new BhakootKoota().score(boy, girl, t).points());
    }

    @Test
    void bhakootSixEightScoresZero() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        MoonChart girl = chart(Rashi.KANYA, Nakshatra.HASTA, 1); // 6/8
        assertEquals(0.0, new BhakootKoota().score(boy, girl, t).points());
    }

    @Test
    void bhakootOppositeSignIsNotDosha() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        MoonChart girl = chart(Rashi.TULA, Nakshatra.SWATI, 1); // 7/7 — not a dosha
        assertEquals(7.0, new BhakootKoota().score(boy, girl, t).points());
    }

    @Test
    void bhakootSameSignScoresFull() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);
        MoonChart girl = chart(Rashi.MESHA, Nakshatra.BHARANI, 1); // 1/1
        assertEquals(7.0, new BhakootKoota().score(boy, girl, t).points());
    }

    // ---- Nadi (high confidence) ----

    @Test
    void nadiSameIsDoshaZero() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);   // Aadi
        MoonChart girl = chart(Rashi.MITHUNA, Nakshatra.ARDRA, 1);  // Aadi (Ardra falls in Mithuna)
        assertEquals(0.0, new NadiKoota().score(boy, girl, t).points());
    }

    @Test
    void nadiDifferentIsFull() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);  // Aadi
        MoonChart girl = chart(Rashi.MESHA, Nakshatra.BHARANI, 1); // Madhya
        assertEquals(8.0, new NadiKoota().score(boy, girl, t).points());
    }

    // ---- Graha Maitri (bands provisional, but same-lord / mutual-friend logic firm) ----

    @Test
    void grahaMaitriSameLordIsFull() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);     // Mars
        MoonChart girl = chart(Rashi.VRISHCHIKA, Nakshatra.JYESHTHA, 1); // Mars
        assertEquals(5.0, new GrahaMaitriKoota().score(boy, girl, t).points());
    }

    @Test
    void grahaMaitriMutualFriendsIsFull() {
        MoonChart boy = chart(Rashi.SIMHA, Nakshatra.MAGHA, 1);   // Sun
        MoonChart girl = chart(Rashi.KARKA, Nakshatra.PUSHYA, 1); // Moon; Sun-Moon mutual friends
        assertEquals(5.0, new GrahaMaitriKoota().score(boy, girl, t).points());
    }

    @Test
    void grahaMaitriMutualEnemiesIsZero() {
        MoonChart boy = chart(Rashi.SIMHA, Nakshatra.MAGHA, 1);    // Sun
        MoonChart girl = chart(Rashi.MAKARA, Nakshatra.SHRAVANA, 1); // Saturn; Sun-Saturn mutual enemies
        assertEquals(0.0, new GrahaMaitriKoota().score(boy, girl, t).points());
    }

    // ---- Yoni (same / enemy firm; gradations provisional) ----

    @Test
    void yoniSameAnimalIsFull() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);  // Horse
        MoonChart girl = chart(Rashi.KUMBHA, Nakshatra.SHATABHISHA, 1); // Horse
        assertEquals(4.0, new YoniKoota().score(boy, girl, t).points());
    }

    @Test
    void yoniSwornEnemyIsZero() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);  // Horse
        MoonChart girl = chart(Rashi.KANYA, Nakshatra.HASTA, 1);   // Buffalo; Horse-Buffalo enemies
        assertEquals(0.0, new YoniKoota().score(boy, girl, t).points());
    }

    // ---- Gana (matrix provisional) ----

    @Test
    void ganaSameIsFull() {
        MoonChart boy = chart(Rashi.MESHA, Nakshatra.ASHWINI, 1);        // Deva
        MoonChart girl = chart(Rashi.VRISHABHA, Nakshatra.MRIGASHIRA, 4); // Deva (Mrigashira falls in Vrishabha)
        assertEquals(6.0, new GanaKoota().score(boy, girl, t).points());
    }
}
