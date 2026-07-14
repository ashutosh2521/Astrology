package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaCode;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 3 — Tara (max 3). Count Nakshatras in each direction, take mod 9;
 * a remainder of 3 (Vipat), 5 (Pratyak) or 7 (Vadha) is inauspicious.
 * Each auspicious direction scores 1.5.
 *
 * <p>{@code personAValue} / {@code personBValue} carry the numeric
 * remainder (mod 9) each direction produced — this is the raw
 * "Nakshatra distance" the classical rule keys off, and lets a stored
 * result explain itself without knowing the input Nakshatras.
 */
public final class TaraKoota implements KootaScorer {

    public static final double MAX = 3.0;

    public static final String RULE_BOTH_DIRECTIONS_AUSPICIOUS = "MOD9_BOTH_AUSPICIOUS";
    public static final String RULE_ONE_DIRECTION_AUSPICIOUS = "MOD9_ONE_AUSPICIOUS";
    public static final String RULE_NEITHER_DIRECTION_AUSPICIOUS = "MOD9_NEITHER_AUSPICIOUS";

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        int rBoyToGirl = boy.nakshatra().countTo(girl.nakshatra()) % 9;
        int rGirlToBoy = girl.nakshatra().countTo(boy.nakshatra()) % 9;
        boolean boyOk = auspicious(rBoyToGirl);
        boolean girlOk = auspicious(rGirlToBoy);
        double points = (boyOk ? 1.5 : 0.0) + (girlOk ? 1.5 : 0.0);
        String rule;
        if (boyOk && girlOk) rule = RULE_BOTH_DIRECTIONS_AUSPICIOUS;
        else if (boyOk || girlOk) rule = RULE_ONE_DIRECTION_AUSPICIOUS;
        else rule = RULE_NEITHER_DIRECTION_AUSPICIOUS;

        String detail = "boy→girl count mod 9 = %d (%s), girl→boy = %d (%s)".formatted(
                rBoyToGirl, boyOk ? "good" : "bad", rGirlToBoy, girlOk ? "good" : "bad");
        return new KootaScore(
                KootaCode.TARA, "Tara",
                Integer.toString(rBoyToGirl), Integer.toString(rGirlToBoy),
                points, MAX, false, rule, detail);
    }

    private static boolean auspicious(int remainder) {
        return remainder != 3 && remainder != 5 && remainder != 7;
    }
}
