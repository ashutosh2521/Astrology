package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaCode;
import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.Titles;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Varna;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * Koota 1 — Varna (max 1). Point awarded when the boy's tier is >= the girl's.
 *
 * <p>{@code ruleApplied} distinguishes the two branches so downstream
 * consumers can translate them independently.
 */
public final class VarnaKoota implements KootaScorer {

    public static final double MAX = 1.0;

    /** Boy's Varna rank is at least the girl's — score = 1. */
    public static final String RULE_BOY_TIER_GTE_GIRL = "BOY_TIER_GTE_GIRL";
    /** Boy's Varna rank is below the girl's — score = 0. */
    public static final String RULE_BOY_TIER_BELOW_GIRL = "BOY_TIER_BELOW_GIRL";

    @Override
    public KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables t) {
        Varna boyVarna = t.varna(boy.rashi());
        Varna girlVarna = t.varna(girl.rashi());
        boolean boyGte = boyVarna.rank() >= girlVarna.rank();
        double points = boyGte ? 1.0 : 0.0;
        String rule = boyGte ? RULE_BOY_TIER_GTE_GIRL : RULE_BOY_TIER_BELOW_GIRL;
        String detail = "Boy %s (tier %d) vs girl %s (tier %d): %s".formatted(
                boyVarna, boyVarna.rank(), girlVarna, girlVarna.rank(),
                boyGte ? "boy tier >= girl tier" : "boy tier below girl tier");
        return new KootaScore(
                KootaCode.VARNA, "Varna",
                Titles.titleCase(boyVarna), Titles.titleCase(girlVarna),
                points, MAX, false, rule, detail);
    }
}
