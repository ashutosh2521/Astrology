package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 7 — Bhakoot (max 7). All-or-nothing on the Rashi-to-Rashi distance: the
 * dosha pairs (2/12, 5/9, 6/8) score 0, everything else 7.
 *
 * <p>This scorer reports raw points only. Whether a present dosha is *cancelled*
 * (which affects the overall verdict) is decided by the engine, which has access to
 * the lords needed for the cancellation rules.
 */
public final class BhakootKoota implements KootaScorer {

    public static final double MAX = 7.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        int countBoyToGirl = boy.rashi().countTo(girl.rashi());
        int countGirlToBoy = girl.rashi().countTo(boy.rashi());
        boolean dosha = t.isBhakootDoshaPair(countBoyToGirl, countGirlToBoy);
        double points = dosha ? 0.0 : t.bhakootPoints();
        String detail = "Rashi counts %d/%d → %s".formatted(
                countBoyToGirl, countGirlToBoy, dosha ? "Bhakoot dosha (0)" : "no dosha (7)");
        return new KootaScore("Bhakoot", points, MAX, detail);
    }
}
