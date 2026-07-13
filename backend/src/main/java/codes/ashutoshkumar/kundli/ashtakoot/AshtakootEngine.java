package codes.ashutoshkumar.kundli.ashtakoot;

import codes.ashutoshkumar.kundli.ashtakoot.koota.BhakootKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.GanaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.GrahaMaitriKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.KootaScorer;
import codes.ashutoshkumar.kundli.ashtakoot.koota.NadiKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.TaraKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.VarnaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.VashyaKoota;
import codes.ashutoshkumar.kundli.ashtakoot.koota.YoniKoota;
import codes.ashutoshkumar.kundli.ashtakoot.model.Graha;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nadi;
import codes.ashutoshkumar.kundli.ashtakoot.model.Relation;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the full 8-koota Ashtakoot match and applies dosha cancellation.
 *
 * <p>The spec is explicit that raw point totals alone yield "technically-scored but
 * practically-wrong verdicts" — so the engine computes the 36-point total AND the
 * Nadi/Bhakoot dosha statuses (present + cancelled) and folds both into the verdict.
 *
 * <p>Convention note: cancellation nullifies a dosha's effect on the *verdict* but
 * does NOT restore the koota's raw points (a cancelled Nadi dosha still scores 0/8).
 * Whether points should be restored is a documented VERIFY item.
 */
public final class AshtakootEngine {

    public static final double MAX_POINTS = 36.0;
    /** Traditional minimum total for an acceptable match. */
    public static final double MIN_ACCEPTABLE = 18.0;

    private final ReferenceTables tables;
    private final List<KootaScorer> scorers;

    public AshtakootEngine(ReferenceTables tables) {
        this.tables = tables;
        this.scorers = List.of(
                new VarnaKoota(), new VashyaKoota(), new TaraKoota(), new YoniKoota(),
                new GrahaMaitriKoota(), new GanaKoota(), new BhakootKoota(), new NadiKoota());
    }

    /** Load reference tables from the classpath and build a ready engine. */
    public static AshtakootEngine create() {
        return new AshtakootEngine(ReferenceTables.load());
    }

    public AshtakootResult match(MoonChart boy, MoonChart girl) {
        List<KootaScore> kootaScores = new ArrayList<>();
        double total = 0.0;
        for (KootaScorer scorer : scorers) {
            KootaScore s = scorer.score(boy, girl, tables);
            kootaScores.add(s);
            total += s.points();
        }

        List<DoshaStatus> doshas = List.of(nadiDosha(boy, girl), bhakootDosha(boy, girl));
        String verdict = verdict(total, doshas);
        return new AshtakootResult(kootaScores, total, MAX_POINTS, doshas, verdict);
    }

    private DoshaStatus nadiDosha(MoonChart boy, MoonChart girl) {
        Nadi boyNadi = tables.nadi(boy.nakshatra());
        Nadi girlNadi = tables.nadi(girl.nakshatra());
        boolean present = boyNadi == girlNadi;
        if (!present) {
            return new DoshaStatus("Nadi", false, false, "Different Nadi — no dosha");
        }
        boolean sameRashi = boy.rashi() == girl.rashi();
        boolean sameNakshatra = boy.nakshatra() == girl.nakshatra();
        if (tables.nadiCancelSameRashiDiffNakshatra() && sameRashi && !sameNakshatra) {
            return new DoshaStatus("Nadi", true, true, "Cancelled: same Rashi, different Nakshatra");
        }
        if (tables.nadiCancelSameNakshatraDiffPada() && sameNakshatra && boy.pada() != girl.pada()) {
            return new DoshaStatus("Nadi", true, true, "Cancelled: same Nakshatra, different Pada");
        }
        return new DoshaStatus("Nadi", true, false, "Same Nadi (%s) — dosha applies".formatted(boyNadi));
    }

    private DoshaStatus bhakootDosha(MoonChart boy, MoonChart girl) {
        int countBoyToGirl = boy.rashi().countTo(girl.rashi());
        int countGirlToBoy = girl.rashi().countTo(boy.rashi());
        boolean present = tables.isBhakootDoshaPair(countBoyToGirl, countGirlToBoy);
        if (!present) {
            return new DoshaStatus("Bhakoot", false, false, "Rashi distance not a dosha pair");
        }
        Graha boyLord = tables.lord(boy.rashi());
        Graha girlLord = tables.lord(girl.rashi());
        if (tables.bhakootCancelSameLord() && boyLord == girlLord) {
            return new DoshaStatus("Bhakoot", true, true,
                    "Cancelled: both signs share lord %s".formatted(boyLord));
        }
        if (tables.bhakootCancelLordsMutualFriends()
                && tables.relation(boyLord, girlLord) == Relation.FRIEND
                && tables.relation(girlLord, boyLord) == Relation.FRIEND) {
            return new DoshaStatus("Bhakoot", true, true,
                    "Cancelled: lords %s and %s are mutual friends".formatted(boyLord, girlLord));
        }
        return new DoshaStatus("Bhakoot", true, false, "Bhakoot dosha applies");
    }

    private static String verdict(double total, List<DoshaStatus> doshas) {
        String band;
        if (total < MIN_ACCEPTABLE) {
            band = "Not recommended (below %.0f)".formatted(MIN_ACCEPTABLE);
        } else if (total < 25) {
            band = "Acceptable";
        } else if (total < 33) {
            band = "Good";
        } else {
            band = "Excellent";
        }

        List<String> effective = new ArrayList<>();
        for (DoshaStatus d : doshas) {
            if (d.isEffective()) {
                effective.add(d.name());
            }
        }
        String score = "%.1f/%.0f".formatted(total, MAX_POINTS);
        if (effective.isEmpty()) {
            return "%s — %s (no effective dosha)".formatted(score, band);
        }
        return "%s — %s, but %s dosha applies (not cancelled); review carefully"
                .formatted(score, band, String.join(" & ", effective));
    }
}
