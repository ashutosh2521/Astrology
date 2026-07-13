package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 3 — Tara (max 3). Count Nakshatras in each direction, take mod 9; a remainder
 * of 3 (Vipat), 5 (Pratyak) or 7 (Vadha) is inauspicious. Each auspicious direction
 * scores 1.5.
 */
public final class TaraKoota implements KootaScorer {

    public static final double MAX = 3.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        int rBoyToGirl = boy.nakshatra().countTo(girl.nakshatra()) % 9;
        int rGirlToBoy = girl.nakshatra().countTo(boy.nakshatra()) % 9;
        double points = half(rBoyToGirl) + half(rGirlToBoy);
        String detail = "boy→girl count mod 9 = %d (%s), girl→boy = %d (%s)".formatted(
                rBoyToGirl, auspicious(rBoyToGirl) ? "good" : "bad",
                rGirlToBoy, auspicious(rGirlToBoy) ? "good" : "bad");
        return new KootaScore("Tara", points, MAX, detail);
    }

    private static double half(int remainder) {
        return auspicious(remainder) ? 1.5 : 0.0;
    }

    private static boolean auspicious(int remainder) {
        return remainder != 3 && remainder != 5 && remainder != 7;
    }
}
