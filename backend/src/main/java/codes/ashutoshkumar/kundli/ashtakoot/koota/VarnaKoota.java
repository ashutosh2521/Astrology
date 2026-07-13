package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Varna;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/** Koota 1 — Varna (max 1). Point awarded when the boy's tier is >= the girl's. */
public final class VarnaKoota implements KootaScorer {

    public static final double MAX = 1.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Varna boyVarna = t.varna(boy.rashi());
        Varna girlVarna = t.varna(girl.rashi());
        double points = boyVarna.rank() >= girlVarna.rank() ? 1.0 : 0.0;
        String detail = "Boy %s (tier %d) vs girl %s (tier %d): %s".formatted(
                boyVarna, boyVarna.rank(), girlVarna, girlVarna.rank(),
                points > 0 ? "boy tier >= girl tier" : "boy tier below girl tier");
        return new KootaScore("Varna", points, MAX, detail);
    }
}
