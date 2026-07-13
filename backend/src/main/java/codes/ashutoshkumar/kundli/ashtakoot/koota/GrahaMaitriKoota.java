package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.Graha;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Relation;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 5 — Graha Maitri (max 5). Compares the two Moon-sign lords' mutual natural
 * friendship. The relationship in each direction is combined into a band score.
 * NOTE: the band values are provisional pending Drik Panchang verification.
 */
public final class GrahaMaitriKoota implements KootaScorer {

    public static final double MAX = 5.0;

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Graha boyLord = t.lord(boy.rashi());
        Graha girlLord = t.lord(girl.rashi());

        // Same lord (or same sign) is maximal compatibility.
        if (boyLord == girlLord) {
            return new KootaScore("Graha Maitri", MAX, MAX,
                    "Both Moon signs share lord %s → full".formatted(boyLord));
        }

        Relation boyToGirl = t.relation(boyLord, girlLord);
        Relation girlToBoy = t.relation(girlLord, boyLord);
        String bandKey = bandKey(boyToGirl, girlToBoy);
        double points = t.grahaMaitriBand(bandKey);
        String detail = "%s→%s %s, %s→%s %s (band %s) → %.1f/%.0f".formatted(
                boyLord, girlLord, boyToGirl, girlLord, boyLord, girlToBoy, bandKey, points, MAX);
        return new KootaScore("Graha Maitri", points, MAX, detail);
    }

    /**
     * Order-independent band key: e.g. FRIEND + NEUTRAL and NEUTRAL + FRIEND both map
     * to "FRIEND_NEUTRAL", matching the keys in rashi_to_lord.json.
     */
    private static String bandKey(Relation a, Relation b) {
        // Rank so the key ordering is stable: FRIEND < NEUTRAL < ENEMY as declared.
        if (a.ordinal() <= b.ordinal()) {
            return a + "_" + b;
        }
        return b + "_" + a;
    }
}
