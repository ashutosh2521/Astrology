package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaCode;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;
import java.util.Set;

/**
 * Koota 7 — Bhakoot (max 7). All-or-nothing on the Rashi-to-Rashi distance: the
 * dosha pairs (2/12, 5/9, 6/8) score 0, everything else 7.
 *
 * <p>This scorer reports raw points AND {@code doshaPresent = true} when
 * the distance is one of the three dosha pairs. Whether that present
 * dosha is <em>cancelled</em> (which affects the overall verdict) is
 * decided by the engine, which has access to the lords needed for the
 * cancellation rules and reports it separately via {@link
 * codes.ashutoshkumar.kundli.ashtakoot.DoshaStatus}.
 *
 * <p>{@code ruleApplied} names the specific dosha pair when present, so
 * a translation layer can render each with its distinct Hindi label
 * ({@code Dwir-dwadash}, {@code Nav-pancham}, {@code Shad-ashtak}) if desired.
 */
public final class BhakootKoota implements KootaScorer {

    public static final double MAX = 7.0;

    public static final String RULE_NO_DOSHA = "NO_DOSHA_DISTANCE";
    public static final String RULE_DOSHA_2_12 = "DOSHA_PAIR_2_12";
    public static final String RULE_DOSHA_5_9 = "DOSHA_PAIR_5_9";
    public static final String RULE_DOSHA_6_8 = "DOSHA_PAIR_6_8";

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        int countBoyToGirl = boy.rashi().countTo(girl.rashi());
        int countGirlToBoy = girl.rashi().countTo(boy.rashi());
        boolean dosha = t.isBhakootDoshaPair(countBoyToGirl, countGirlToBoy);
        double points = dosha ? 0.0 : t.bhakootPoints();
        String rule = dosha ? doshaPairCode(countBoyToGirl, countGirlToBoy) : RULE_NO_DOSHA;
        String detail = "Rashi counts %d/%d → %s".formatted(
                countBoyToGirl, countGirlToBoy, dosha ? "Bhakoot dosha (0)" : "no dosha (7)");
        return new KootaScore(
                KootaCode.BHAKOOT, "Bhakoot",
                Integer.toString(countBoyToGirl), Integer.toString(countGirlToBoy),
                points, MAX, dosha, rule, detail);
    }

    /**
     * Which of the three dosha pairs fired. Uses set-equality because Bhakoot
     * counts are direction-symmetric (a 6/8 pair is a dosha whether you count
     * boy→girl or girl→boy first).
     */
    private static String doshaPairCode(int a, int b) {
        Set<Integer> pair = Set.of(a, b);
        if (pair.equals(Set.of(2, 12))) return RULE_DOSHA_2_12;
        if (pair.equals(Set.of(5, 9))) return RULE_DOSHA_5_9;
        if (pair.equals(Set.of(6, 8))) return RULE_DOSHA_6_8;
        // Unreachable: isBhakootDoshaPair already restricted to these three.
        throw new IllegalStateException("Unknown Bhakoot dosha pair: " + a + "/" + b);
    }
}
