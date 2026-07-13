package codes.ashutoshkumar.kundli.ashtakoot.koota;

import codes.ashutoshkumar.kundli.ashtakoot.KootaScore;
import codes.ashutoshkumar.kundli.ashtakoot.model.MoonChart;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;

/**
 * One koota's scoring rule, as a pure function of the two charts and the reference
 * tables. Kept pure and table-driven so each koota is unit-testable in isolation —
 * no ephemeris, no Spring, no shared state — which is exactly what lets the
 * regression suite pin each koota against Drik Panchang independently.
 *
 * <p>Directionality: {@code boy} and {@code girl} are passed explicitly because
 * several kootas (Varna, Gana, Graha Maitri, Tara) are not symmetric.
 */
public interface KootaScorer {
    KootaScore score(MoonChart boy, MoonChart girl, ReferenceTables tables);
}
