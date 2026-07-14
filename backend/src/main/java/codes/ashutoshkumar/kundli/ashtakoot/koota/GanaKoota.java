package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaCode;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.Titles;
import codes.ashutoshkumar.kundli.ashtakoot.model.Gana;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 6 — Gana (max 6). 3×3 temperament matrix, stored row=boy/col=girl so it can
 * be directional.
 *
 * <p>The matrix values are provisional pending Drik Panchang verification
 * (backend/VERIFICATION.md #5). {@code ruleApplied} distinguishes the
 * same-Gana branch from any cross-Gana matrix lookup.
 */
public final class GanaKoota implements KootaScorer {

    public static final double MAX = 6.0;

    public static final String RULE_SAME_GANA = "SAME_GANA";
    public static final String RULE_CROSS_GANA_MATRIX = "CROSS_GANA_MATRIX_LOOKUP";

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Gana boyGana = t.gana(boy.nakshatra());
        Gana girlGana = t.gana(girl.nakshatra());
        double points = t.ganaScore(boyGana, girlGana);
        String rule = boyGana == girlGana ? RULE_SAME_GANA : RULE_CROSS_GANA_MATRIX;
        String detail = "Boy %s vs girl %s → %.0f/%.0f".formatted(boyGana, girlGana, points, MAX);
        return new KootaScore(
                KootaCode.GANA, "Gana",
                Titles.titleCase(boyGana), Titles.titleCase(girlGana),
                points, MAX, false, rule, detail);
    }
}
