package codes.ashutoshkumar.kundli.manglik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Full-coverage tests for the Manglik engine.
 *
 * <p>The rule is small enough that we can test every branch: Mars in every
 * one of the 12 houses × 3 reference points = 36 combinations, each of
 * which either fires or doesn't according to the {1, 2, 4, 7, 8, 12} set.
 * Plus the couple-combine matrix (NOT×NOT, NOT×MANGLIK, MANGLIK×NOT,
 * MANGLIK×MANGLIK).
 *
 * <p>All longitudes are constructed as "middle of Rashi N", i.e.
 * {@code (N-1) * 30 + 15} degrees. That keeps every input safely away
 * from a sign boundary; boundary sensitivity is a chart-layer concern
 * (see BirthChartService boundary warnings), not a rule-engine concern.
 */
class ManglikEngineTest {

    private final ManglikEngine engine = ManglikEngine.create();

    /** Longitude in the middle of Rashi n (1..12). */
    private static double midOfRashi(int n) {
        return (n - 1) * 30.0 + 15.0;
    }

    // ---- Rules-file loading + shape ----

    @Test
    void loadsAndValidatesRulesAtConstruction() {
        assertEquals("manglik-v1.2", engine.ruleVersion());
        // The exact v1 trigger houses fixed by the spec.
        assertEquals(Set.of(1, 2, 4, 7, 8, 12), engine.triggerHouses());
        // v1.1 grading buckets: {1} → PARTIAL, {2, 3} → full MANGLIK.
        // NOT_MANGLIK (count 0) is implicit.
        assertEquals(Set.of(1), engine.partialManglikCounts());
        assertEquals(Set.of(2, 3), engine.fullManglikCounts());
        // v1.2 Mars-strength cancellations: Mesha (1) + Vrishchika (8) = own;
        // Makara (10) = exalted; Karka (4) = debilitated.
        assertEquals("MARS_IN_OWN_SIGN", engine.cancellationByMarsSign().get(1));
        assertEquals("MARS_IN_OWN_SIGN", engine.cancellationByMarsSign().get(8));
        assertEquals("MARS_EXALTED", engine.cancellationByMarsSign().get(10));
        assertEquals("MARS_DEBILITATED", engine.cancellationByMarsSign().get(4));
        assertEquals(4, engine.cancellationByMarsSign().size());
    }

    // ---- Per-house presence: 12 × 3 = 36 combinations exhaustive ----

    /**
     * For each house 1..12 place Mars there relative to the LAGNA reference
     * and confirm presence matches the v1 trigger set.
     */
    @ParameterizedTest(name = "Mars in house {0} from Lagna")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void marsInHouseNFromLagna(int house) {
        // Ascendant fixed at Mesha (Rashi 1). Mars placed to land in `house`.
        // For whole-sign math: house = ((marsRashi - lagnaRashi + 12) % 12) + 1
        // => marsRashi = ((house - 1 + lagnaRashi - 1) % 12) + 1
        int lagnaRashi = 1;
        int marsRashi = ((house - 1 + lagnaRashi - 1) % 12) + 1;
        ManglikStatus s = engine.evaluate(
                midOfRashi(lagnaRashi),
                6,                        // Moon in Kanya — a safe non-trigger position from lagna 1
                midOfRashi(marsRashi),
                midOfRashi(6));           // Venus in Kanya — same safe reason

        assertEquals(house, s.fromLagna().marsHouse(),
                "computed Mars house must match the placement we constructed");
        boolean shouldTrigger = engine.triggerHouses().contains(house);
        assertEquals(shouldTrigger, s.fromLagna().present(),
                "Lagna present flag must follow the v1 trigger set for house " + house);
    }

    @ParameterizedTest(name = "Mars in house {0} from Moon")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void marsInHouseNFromMoon(int house) {
        // Moon fixed at Karka (Rashi 4). Mars placed to land in `house`
        // from the Moon.
        int moonRashi = 4;
        int marsRashi = ((house - 1 + moonRashi - 1) % 12) + 1;

        // We also need Lagna and Venus to NOT trigger, or the assertions
        // isolate Moon. Pick lagna=marsRashi so Mars is in house 1 from
        // lagna (which DOES trigger) — instead pick lagna at a distance
        // that puts Mars in house 3, 5, 6, 9, 10 or 11 for this scenario.
        // Simplest: put Ascendant at marsRashi - 2 (mod 12), which
        // places Mars in house 3 from lagna (non-trigger) for every case.
        int lagnaRashi = ((marsRashi - 3 + 12) % 12) + 1;   // Mars will be in house 3 from Lagna
        int venusRashi = lagnaRashi;                         // Venus co-located with Ascendant → Mars house 3 from Venus too

        ManglikStatus s = engine.evaluate(
                midOfRashi(lagnaRashi),
                moonRashi,
                midOfRashi(marsRashi),
                midOfRashi(venusRashi));

        assertEquals(house, s.fromMoon().marsHouse());
        boolean shouldTrigger = engine.triggerHouses().contains(house);
        assertEquals(shouldTrigger, s.fromMoon().present(),
                "Moon present flag must follow the v1 trigger set for house " + house);
        // Isolation guard.
        assertFalse(s.fromLagna().present(),
                "Lagna reference must not fire in this isolation setup");
        assertFalse(s.fromVenus().present(),
                "Venus reference must not fire in this isolation setup");
    }

    @ParameterizedTest(name = "Mars in house {0} from Venus")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void marsInHouseNFromVenus(int house) {
        int venusRashi = 7; // Tula — arbitrary
        int marsRashi = ((house - 1 + venusRashi - 1) % 12) + 1;

        // Isolate Venus by putting Lagna and Moon so Mars lands in house 3 (non-trigger).
        int lagnaRashi = ((marsRashi - 3 + 12) % 12) + 1;
        int moonRashi = lagnaRashi;

        ManglikStatus s = engine.evaluate(
                midOfRashi(lagnaRashi),
                moonRashi,
                midOfRashi(marsRashi),
                midOfRashi(venusRashi));

        assertEquals(house, s.fromVenus().marsHouse());
        boolean shouldTrigger = engine.triggerHouses().contains(house);
        assertEquals(shouldTrigger, s.fromVenus().present());
        assertFalse(s.fromLagna().present(), "Lagna must be isolated in this setup");
        assertFalse(s.fromMoon().present(), "Moon must be isolated in this setup");
    }

    // ---- Overall per-person state derived from the three references ----

    @Test
    void neitherPartnerTriggeredIsNotManglik() {
        // Mars in house 3 from all three references — trigger houses are {1,2,4,7,8,12}.
        ManglikStatus s = engine.evaluate(
                midOfRashi(1),      // Lagna Mesha
                1,                  // Moon Mesha
                midOfRashi(3),      // Mars Mithuna → house 3 from lagna, moon, venus (if venus also Mesha)
                midOfRashi(1));     // Venus Mesha

        assertEquals(ManglikState.NOT_MANGLIK, s.status());
        assertTrue(s.triggeredReferences().isEmpty());
    }

    @Test
    void singleReferenceTriggeredIsPartialManglik() {
        // Mars in Vrishabha (2 — NOT a cancellation sign, so Anshik still fires):
        //   from Lagna Mesha → house 2 (trigger)
        //   from Moon Kanya → house 9 (not trigger)
        //   from Venus Kanya → house 9 (not trigger)
        // Exactly one reference triggered → PARTIAL_MANGLIK (Anshik).
        ManglikStatus s = engine.evaluate(
                midOfRashi(1),      // Lagna Mesha
                6,                  // Moon Kanya
                midOfRashi(2),      // Mars Vrishabha → 2nd from Lagna (trigger)
                midOfRashi(6));     // Venus Kanya

        assertEquals(ManglikState.PARTIAL_MANGLIK, s.status(),
                "exactly one triggered reference → PARTIAL_MANGLIK (Anshik)");
        assertEquals(List.of(ReferencePoint.LAGNA), s.triggeredReferences());
        assertTrue(s.fromLagna().present());
        assertFalse(s.fromMoon().present());
        assertFalse(s.fromVenus().present());
        assertEquals(2, s.fromLagna().marsHouse());
        assertTrue(s.cancellations().isEmpty(),
                "Mars in Vrishabha is not a cancellation sign");
    }

    @Test
    void twoReferencesTriggeredIsFullManglik() {
        // Ascendant Mesha, Moon Mesha (so Mars-in-Vrishabha triggers both);
        // Venus in Kanya so Mars in 9th from Venus (safe).
        ManglikStatus s = engine.evaluate(
                midOfRashi(1),      // Lagna Mesha
                1,                  // Moon Mesha  → same as Lagna
                midOfRashi(2),      // Mars Vrishabha → 2nd from Mesha (trigger from both)
                midOfRashi(6));     // Venus Kanya

        assertEquals(ManglikState.MANGLIK, s.status(),
                "two triggered references → full MANGLIK");
        assertEquals(
                List.of(ReferencePoint.LAGNA, ReferencePoint.MOON),
                s.triggeredReferences());
        assertTrue(s.fromLagna().present());
        assertTrue(s.fromMoon().present());
        assertFalse(s.fromVenus().present());
    }

    @Test
    void threeReferencesTriggeredIsFullManglik() {
        // Ascendant, Moon and Venus all in Mesha; Mars in Vrishabha (Rashi 2).
        // Mars in 2nd from all three → all trigger → full MANGLIK.
        // (Vrishabha is chosen because 4 (Karka) is a debilitation cancellation.)
        ManglikStatus s = engine.evaluate(
                midOfRashi(1), 1, midOfRashi(2), midOfRashi(1));

        assertEquals(ManglikState.MANGLIK, s.status());
        assertEquals(
                List.of(ReferencePoint.LAGNA, ReferencePoint.MOON, ReferencePoint.VENUS),
                s.triggeredReferences());
        assertEquals(2, s.fromLagna().marsHouse());
        assertEquals(2, s.fromMoon().marsHouse());
        assertEquals(2, s.fromVenus().marsHouse());
    }

    @Test
    void ruleVersionIsAttachedToEveryResult() {
        ManglikStatus s = engine.evaluate(midOfRashi(1), 1, midOfRashi(3), midOfRashi(1));
        assertEquals("manglik-v1.2", s.ruleVersion());
        assertTrue(s.cancellations().isEmpty(),
                "Mars in Mithuna is not a cancellation sign — cancellations empty");
    }

    // ---- Couple combine ----

    @Test
    void bothNotManglikYieldsNeitherManglik() {
        ManglikStatus safe = engine.evaluate(midOfRashi(1), 1, midOfRashi(3), midOfRashi(1));
        ManglikCompatibility c = engine.combine(safe, safe);
        assertEquals(ManglikCompatibility.Compatibility.NEITHER_MANGLIK, c.compatibility());
        assertEquals("manglik-v1.2", c.ruleVersion());
    }

    /**
     * Anshik Manglik (partial, one reference triggered) is still not "safe"
     * for a couple result. Any Manglik presence — full or partial — routes
     * to detailed review. The spec's "do not automatically declare Manglik-
     * Manglik safe" applies to Anshik too.
     */
    @Test
    void partialManglikPartnerStillRequiresDetailedReview() {
        // Person A: partial Manglik. Mars in Vrishabha (2 — non-cancel), Lagna
        // Mesha → house 2 (trigger); Moon and Venus in Kanya (6) → house 9 (safe).
        ManglikStatus partial = engine.evaluate(
                midOfRashi(1), 6, midOfRashi(2), midOfRashi(6));
        assertEquals(ManglikState.PARTIAL_MANGLIK, partial.status(),
                "sanity: constructed partial-Manglik case");

        // Person B: fully clean.
        ManglikStatus safe = engine.evaluate(midOfRashi(1), 1, midOfRashi(3), midOfRashi(1));

        ManglikCompatibility c = engine.combine(partial, safe);
        assertEquals(ManglikCompatibility.Compatibility.REQUIRES_DETAILED_REVIEW,
                c.compatibility(),
                "one partial + one clean must still route to detailed review — Anshik is not safe");
    }

    @Test
    void bothPartialManglikStillRequiresDetailedReview() {
        ManglikStatus partial = engine.evaluate(
                midOfRashi(1), 6, midOfRashi(2), midOfRashi(6));
        ManglikCompatibility c = engine.combine(partial, partial);
        assertEquals(ManglikCompatibility.Compatibility.REQUIRES_DETAILED_REVIEW,
                c.compatibility(),
                "two Anshik-Manglik partners must never be auto-declared safe");
    }

    /**
     * The spec's core anti-pattern: two Manglik partners must NOT be auto-declared safe.
     * The classical "Manglik-Manglik cancel" folk rule is deliberately not applied.
     */
    @Test
    void bothManglikStillRequiresDetailedReview() {
        // Mars in Vrishabha (2 — non-cancel), Lagna+Moon+Venus in Mesha → house 2
        // from all three → three triggers → full MANGLIK.
        ManglikStatus manglik = engine.evaluate(midOfRashi(1), 1, midOfRashi(2), midOfRashi(1));
        assertEquals(ManglikState.MANGLIK, manglik.status(),
                "sanity: constructed full-Manglik case");
        ManglikCompatibility c = engine.combine(manglik, manglik);
        assertEquals(ManglikCompatibility.Compatibility.REQUIRES_DETAILED_REVIEW,
                c.compatibility(),
                "Two Manglik partners must never be auto-declared compatible");
    }

    @ParameterizedTest(name = "personA={0} personB={1} → detailed review")
    @CsvSource({
            "MANGLIK,NOT_MANGLIK",
            "NOT_MANGLIK,MANGLIK",
    })
    void asymmetricManglikRoutesToDetailedReview(String aState, String bState) {
        ManglikStatus a = manglikOrSafe(aState);
        ManglikStatus b = manglikOrSafe(bState);
        ManglikCompatibility c = engine.combine(a, b);
        assertEquals(ManglikCompatibility.Compatibility.REQUIRES_DETAILED_REVIEW,
                c.compatibility());
    }

    private ManglikStatus manglikOrSafe(String state) {
        return switch (state) {
            // Vrishabha (2) — non-cancel, gives triggers from Mesha lagna/moon/venus.
            case "MANGLIK"     -> engine.evaluate(midOfRashi(1), 1, midOfRashi(2), midOfRashi(1));
            case "NOT_MANGLIK" -> engine.evaluate(midOfRashi(1), 1, midOfRashi(3), midOfRashi(1));
            default -> throw new IllegalArgumentException("unknown state: " + state);
        };
    }

    // ---- Input validation ----

    @Test
    void moonRashiOutOfRangeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.evaluate(midOfRashi(1), 0, midOfRashi(3), midOfRashi(1)));
        assertThrows(IllegalArgumentException.class,
                () -> engine.evaluate(midOfRashi(1), 13, midOfRashi(3), midOfRashi(1)));
    }

    @Test
    void longitudesWrapCorrectlyAcrossZero() {
        // Longitude 361.5 should be treated as 1.5° into Mesha (Rashi 1).
        ManglikStatus s = engine.evaluate(1.5, 1, 361.5, 1.5);
        // Mars in Mesha (1) with Ascendant in Mesha (1) → house 1 → trigger.
        assertEquals(1, s.fromLagna().marsHouse());
        assertTrue(s.fromLagna().present());
    }

    @Test
    void negativeLongitudesWrapCorrectly() {
        // Longitude -0.5 is equivalent to 359.5 → last Rashi, Meena (12).
        ManglikStatus s = engine.evaluate(-0.5, 12, midOfRashi(3), -0.5);
        // Ascendant in Meena (12); Mars in Mithuna (3) → house = ((3-12+12) % 12) + 1 = 4 → trigger.
        assertEquals(4, s.fromLagna().marsHouse());
        assertTrue(s.fromLagna().present());
    }

    // ---- v1.2 Mars-strength cancellations ----

    /**
     * Mars in its own sign (Mesha/Aries) cancels Manglik entirely even when
     * every reference point would otherwise trigger. Raw per-reference fields
     * are preserved for transparency; overall status is NOT_MANGLIK; the
     * cancellation code is populated.
     */
    @Test
    void marsInOwnSignAriesCancelsManglik() {
        // All three references in Mesha; Mars in Mesha → every reference
        // would show house 1 (trigger). Cancellation overrides.
        ManglikStatus s = engine.evaluate(
                midOfRashi(1), 1, midOfRashi(1), midOfRashi(1));

        assertEquals(ManglikState.NOT_MANGLIK, s.status(),
                "Mars in own sign (Mesha) cancels Manglik");
        assertEquals(List.of("MARS_IN_OWN_SIGN"), s.cancellations());
        // Transparency: raw fields still show what would have triggered.
        assertTrue(s.fromLagna().present(), "raw fromLagna.present preserved for the report");
        assertTrue(s.fromMoon().present());
        assertTrue(s.fromVenus().present());
        assertEquals(1, s.fromLagna().marsHouse());
    }

    @Test
    void marsInOwnSignScorpioCancelsManglik() {
        // Lagna Mesha, Mars in Vrishchika → house 8 (would trigger). Cancelled.
        ManglikStatus s = engine.evaluate(
                midOfRashi(1), 1, midOfRashi(8), midOfRashi(1));
        assertEquals(ManglikState.NOT_MANGLIK, s.status());
        assertEquals(List.of("MARS_IN_OWN_SIGN"), s.cancellations());
        assertEquals(8, s.fromLagna().marsHouse());
        assertTrue(s.fromLagna().present());
    }

    @Test
    void marsExaltedInCapricornCancelsManglik() {
        // Lagna Mesha, Mars in Makara → house 10 (not itself a trigger); Moon
        // in Karka → house 7 (trigger); Venus in Mithuna → house 8 (trigger).
        // Without cancellation this would be full MANGLIK. Cancelled instead.
        ManglikStatus s = engine.evaluate(
                midOfRashi(1), 4, midOfRashi(10), midOfRashi(3));
        assertEquals(ManglikState.NOT_MANGLIK, s.status());
        assertEquals(List.of("MARS_EXALTED"), s.cancellations());
        assertTrue(s.fromMoon().present(), "would have triggered from Moon before cancellation");
        assertTrue(s.fromVenus().present(), "would have triggered from Venus before cancellation");
    }

    @Test
    void marsDebilitatedInCancerCancelsManglik() {
        // Mars in Karka (debilitated). Lagna Mesha → house 4 (would trigger). Cancelled.
        ManglikStatus s = engine.evaluate(
                midOfRashi(1), 1, midOfRashi(4), midOfRashi(1));
        assertEquals(ManglikState.NOT_MANGLIK, s.status());
        assertEquals(List.of("MARS_DEBILITATED"), s.cancellations());
    }

    @Test
    void nonCancelMarsSignStillProducesManglik() {
        // Sanity — the same "all references in Mesha" setup with Mars moved
        // OFF a cancellation sign must still produce Manglik.
        ManglikStatus s = engine.evaluate(
                midOfRashi(1), 1, midOfRashi(2), midOfRashi(1));
        assertEquals(ManglikState.MANGLIK, s.status(),
                "Mars in Vrishabha (non-cancel) produces MANGLIK as before");
        assertTrue(s.cancellations().isEmpty());
    }

    /**
     * A cancelled personA + a clean personB is still not "safe" for the
     * couple result — but the compatibility flows through both partners'
     * status, and cancelled = NOT_MANGLIK, so this specific pair actually
     * IS clean. Locks that in.
     */
    @Test
    void cancelledPartnerBehavesAsNotManglikInCoupleCompatibility() {
        ManglikStatus cancelledA = engine.evaluate(
                midOfRashi(1), 1, midOfRashi(1), midOfRashi(1)); // Mars in own sign
        ManglikStatus safeB = engine.evaluate(
                midOfRashi(1), 1, midOfRashi(3), midOfRashi(1));
        ManglikCompatibility c = engine.combine(cancelledA, safeB);
        assertEquals(ManglikCompatibility.Compatibility.NEITHER_MANGLIK,
                c.compatibility(),
                "cancelled Manglik is a valid NEITHER_MANGLIK case for the couple result");
    }
}
