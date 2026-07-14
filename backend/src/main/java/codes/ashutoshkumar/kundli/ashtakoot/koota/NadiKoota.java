package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaCode;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.Titles;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nadi;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 8 — Nadi (max 8). Same Nadi = 0 (Nadi dosha), different Nadi = full 8.
 *
 * <p>Reports raw points AND {@code doshaPresent = true} when both partners
 * share a Nadi. Whether the present dosha is cancelled (which affects the
 * verdict, not the koota's own score) is decided by the engine and
 * reported separately via {@link codes.ashutoshkumar.kundli.ashtakoot.DoshaStatus}.
 */
public final class NadiKoota implements KootaScorer {

    public static final double MAX = 8.0;

    public static final String RULE_DIFFERENT_NADI_FULL = "DIFFERENT_NADI_FULL_SCORE";
    public static final String RULE_SAME_NADI_DOSHA = "SAME_NADI_DOSHA";

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Nadi boyNadi = t.nadi(boy.nakshatra());
        Nadi girlNadi = t.nadi(girl.nakshatra());
        boolean dosha = boyNadi == girlNadi;
        double points = dosha ? 0.0 : t.nadiPoints();
        String rule = dosha ? RULE_SAME_NADI_DOSHA : RULE_DIFFERENT_NADI_FULL;
        String detail = "Boy %s vs girl %s → %s".formatted(
                boyNadi, girlNadi, dosha ? "Nadi dosha (0)" : "different Nadi (8)");
        return new KootaScore(
                KootaCode.NADI, "Nadi",
                Titles.titleCase(boyNadi), Titles.titleCase(girlNadi),
                points, MAX, dosha, rule, detail);
    }
}
