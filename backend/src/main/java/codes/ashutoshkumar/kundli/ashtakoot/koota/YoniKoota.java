package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Yoni;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 4 — Yoni (max 4). Animal compatibility. Same animal = 4, sworn enemies = 0.
 * NOTE: the intermediate friend/neutral gradations are provisional pending Drik
 * Panchang verification (see nakshatra_to_yoni.json).
 */
public final class YoniKoota implements KootaScorer {

    public static final double MAX = 4.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Yoni boyYoni = t.yoni(boy.nakshatra());
        Yoni girlYoni = t.yoni(girl.nakshatra());
        double points = t.yoniScore(boyYoni, girlYoni);
        String detail = "Boy %s vs girl %s → %.0f/%.0f".formatted(boyYoni, girlYoni, points, MAX);
        return new KootaScore("Yoni", points, MAX, detail);
    }
}
