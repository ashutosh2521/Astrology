package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nadi;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 8 — Nadi (max 8). Same Nadi = 0 (Nadi dosha), different Nadi = full 8.
 *
 * <p>Reports raw points only; the engine decides whether a present dosha is cancelled.
 */
public final class NadiKoota implements KootaScorer {

    public static final double MAX = 8.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Nadi boyNadi = t.nadi(boy.nakshatra());
        Nadi girlNadi = t.nadi(girl.nakshatra());
        boolean dosha = boyNadi == girlNadi;
        double points = dosha ? 0.0 : t.nadiPoints();
        String detail = "Boy %s vs girl %s → %s".formatted(
                boyNadi, girlNadi, dosha ? "Nadi dosha (0)" : "different Nadi (8)");
        return new KootaScore("Nadi", points, MAX, detail);
    }
}
