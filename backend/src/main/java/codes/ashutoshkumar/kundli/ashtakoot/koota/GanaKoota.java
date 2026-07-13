package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.Gana;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 6 — Gana (max 6). 3×3 temperament matrix, stored row=boy/col=girl so it can
 * be directional. NOTE: the matrix values are provisional pending Drik Panchang.
 */
public final class GanaKoota implements KootaScorer {

    public static final double MAX = 6.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Gana boyGana = t.gana(boy.nakshatra());
        Gana girlGana = t.gana(girl.nakshatra());
        double points = t.ganaScore(boyGana, girlGana);
        String detail = "Boy %s vs girl %s → %.0f/%.0f".formatted(boyGana, girlGana, points, MAX);
        return new KootaScore("Gana", points, MAX, detail);
    }
}
