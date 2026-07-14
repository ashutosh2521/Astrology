package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaCode;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.Titles;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.VashyaGroup;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 2 — Vashya (max 2). 5×5 group-compatibility matrix, partial credit possible.
 *
 * <p>The matrix values are provisional pending Drik Panchang verification
 * (backend/VERIFICATION.md #1–#2); {@code ruleApplied} distinguishes the
 * same-group full-score branch from any cross-group matrix lookup so the
 * frontend can render the two cases distinctly even before the numbers
 * themselves are verified.
 */
public final class VashyaKoota implements KootaScorer {

    public static final double MAX = 2.0;

    public static final String RULE_SAME_VASHYA_GROUP = "SAME_VASHYA_GROUP";
    public static final String RULE_CROSS_GROUP_MATRIX = "CROSS_GROUP_MATRIX_LOOKUP";

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        VashyaGroup boyGroup = t.vashyaGroup(boy.rashi());
        VashyaGroup girlGroup = t.vashyaGroup(girl.rashi());
        double points = t.vashyaScore(boyGroup, girlGroup);
        String rule = boyGroup == girlGroup ? RULE_SAME_VASHYA_GROUP : RULE_CROSS_GROUP_MATRIX;
        String detail = "Boy %s vs girl %s → %.1f/%.0f".formatted(boyGroup, girlGroup, points, MAX);
        return new KootaScore(
                KootaCode.VASHYA, "Vashya",
                Titles.titleCase(boyGroup), Titles.titleCase(girlGroup),
                points, MAX, false, rule, detail);
    }
}
