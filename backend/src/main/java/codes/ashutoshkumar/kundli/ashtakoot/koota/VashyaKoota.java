package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.VashyaGroup;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 2 — Vashya (max 2). 5×5 group-compatibility matrix, partial credit possible.
 * NOTE: the matrix values are provisional pending Drik Panchang verification.
 */
public final class VashyaKoota implements KootaScorer {

    public static final double MAX = 2.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        VashyaGroup boyGroup = t.vashyaGroup(boy.rashi());
        VashyaGroup girlGroup = t.vashyaGroup(girl.rashi());
        double points = t.vashyaScore(boyGroup, girlGroup);
        String detail = "Boy %s vs girl %s → %.1f/%.0f".formatted(boyGroup, girlGroup, points, MAX);
        return new KootaScore("Vashya", points, MAX, detail);
    }
}
