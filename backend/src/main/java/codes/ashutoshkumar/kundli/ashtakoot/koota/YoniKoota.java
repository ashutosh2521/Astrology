package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaCode;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.Titles;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Yoni;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 4 — Yoni (max 4). Animal compatibility. Same animal = 4, sworn enemies = 0.
 *
 * <p>The intermediate friend/neutral gradations are provisional pending Drik
 * Panchang verification (backend/VERIFICATION.md #3). The three rule
 * branches ({@code SAME}, {@code SWORN_ENEMY}, {@code DEFAULT}) are all
 * that the current lookup implements — the eventual
 * FRIEND / NEUTRAL / NON-SWORN-ENEMY split will replace the {@code DEFAULT}
 * branch with several finer ones.
 */
public final class YoniKoota implements KootaScorer {

    public static final double MAX = 4.0;

    public static final String RULE_SAME_YONI = "SAME_YONI_ANIMAL";
    public static final String RULE_SWORN_ENEMY = "SWORN_ENEMY_YONI_PAIR";
    /** Placeholder branch — the friend/neutral gradation is not yet split. */
    public static final String RULE_DEFAULT_YONI_PAIR = "DEFAULT_YONI_PAIR_PROVISIONAL";

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Yoni boyYoni = t.yoni(boy.nakshatra());
        Yoni girlYoni = t.yoni(girl.nakshatra());
        double points = t.yoniScore(boyYoni, girlYoni);
        String rule;
        if (boyYoni == girlYoni) rule = RULE_SAME_YONI;
        else if (points == 0.0) rule = RULE_SWORN_ENEMY;
        else rule = RULE_DEFAULT_YONI_PAIR;

        String detail = "Boy %s vs girl %s → %.0f/%.0f".formatted(boyYoni, girlYoni, points, MAX);
        return new KootaScore(
                KootaCode.YONI, "Yoni",
                Titles.titleCase(boyYoni), Titles.titleCase(girlYoni),
                points, MAX, false, rule, detail);
    }
}
