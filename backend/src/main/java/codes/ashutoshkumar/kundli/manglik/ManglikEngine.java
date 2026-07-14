package codes.ashutoshkumar.kundli.manglik;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Basic Manglik / Kuja Dosha presence engine, v1.0.
 *
 * <p>Rules are loaded from {@code /manglik/manglik_rules.json} at
 * construction time and validated for completeness; a bad rules file
 * fails app boot rather than a match request. Same "catch, don't silently
 * accept" stance the ephemeris service and Ashtakoot engine use.
 *
 * <p>Deliberately pure: takes primitive longitudes + a Moon Rashi number
 * as input, returns a {@link ManglikStatus}. No Spring, no ephemeris
 * client, no persistence. Unit-testable in isolation across every
 * (house, reference point) combination.
 *
 * <p>House math is whole-sign, uniformly for all three reference points:
 * <pre>
 *   refRashi     = floor(refLongitude / 30) + 1              // 1..12
 *   marsRashi    = floor(marsLongitude / 30) + 1
 *   house        = ((marsRashi - refRashi + 12) % 12) + 1    // 1..12
 * </pre>
 * The Moon reference uses the Moon's Rashi directly (already stored on
 * the birth chart entity), avoiding a redundant longitude→sign conversion.
 */
public final class ManglikEngine {

    private static final String RULES_RESOURCE = "/manglik/manglik_rules.json";

    private final String ruleVersion;
    private final Set<Integer> triggerHouses;

    ManglikEngine(String ruleVersion, Set<Integer> triggerHouses) {
        this.ruleVersion = ruleVersion;
        this.triggerHouses = Set.copyOf(triggerHouses);
    }

    /** Load rules from the classpath JSON and construct a ready engine. */
    public static ManglikEngine create() {
        ObjectMapper om = new ObjectMapper();
        JsonNode root = read(om);

        String ruleVersion = requireText(root, "ruleVersion");
        String houseSystem = requireText(root, "houseSystem");
        if (!"WHOLE_SIGN".equals(houseSystem)) {
            throw new IllegalStateException(
                    "manglik_rules.json houseSystem must be WHOLE_SIGN for v1, got: " + houseSystem);
        }

        JsonNode housesNode = root.get("triggerHouses");
        if (housesNode == null || !housesNode.isArray() || housesNode.isEmpty()) {
            throw new IllegalStateException("manglik_rules.json triggerHouses must be a non-empty array");
        }
        Set<Integer> triggerHouses = new java.util.HashSet<>();
        for (JsonNode h : housesNode) {
            int v = h.asInt(-1);
            if (v < 1 || v > 12) {
                throw new IllegalStateException(
                        "manglik_rules.json triggerHouses entry out of range 1..12: " + v);
            }
            triggerHouses.add(v);
        }
        return new ManglikEngine(ruleVersion, triggerHouses);
    }

    private static JsonNode read(ObjectMapper om) {
        try (InputStream in = ManglikEngine.class.getResourceAsStream(RULES_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Manglik rules not found: " + RULES_RESOURCE);
            }
            return om.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + RULES_RESOURCE, e);
        }
    }

    private static String requireText(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw new IllegalStateException(
                    "manglik_rules.json missing required text field: " + field);
        }
        return n.asText();
    }

    /**
     * The rule-version string surfaced on every result. Same value stored
     * in {@code KundliProperties.rules.manglikRuleVersion}.
     */
    public String ruleVersion() {
        return ruleVersion;
    }

    /** For test introspection: the exact trigger-houses set loaded from JSON. */
    Set<Integer> triggerHouses() {
        return triggerHouses;
    }

    /**
     * Evaluate one person.
     *
     * @param ascendantLongitude sidereal longitude of the Ascendant, degrees [0, 360)
     * @param moonRashiNumber    Moon's Rashi as 1..12 (Mesha=1 … Meena=12)
     * @param marsLongitude      sidereal longitude of Mars
     * @param venusLongitude     sidereal longitude of Venus
     */
    public ManglikStatus evaluate(
            double ascendantLongitude,
            int moonRashiNumber,
            double marsLongitude,
            double venusLongitude) {

        if (moonRashiNumber < 1 || moonRashiNumber > 12) {
            throw new IllegalArgumentException(
                    "moonRashiNumber must be 1..12: " + moonRashiNumber);
        }

        int marsRashi = rashiOf(marsLongitude);
        int lagnaRashi = rashiOf(ascendantLongitude);
        int venusRashi = rashiOf(venusLongitude);

        ReferencePointResult fromLagna = check(marsRashi, lagnaRashi);
        ReferencePointResult fromMoon = check(marsRashi, moonRashiNumber);
        ReferencePointResult fromVenus = check(marsRashi, venusRashi);

        List<ReferencePoint> triggered = new ArrayList<>();
        if (fromLagna.present()) triggered.add(ReferencePoint.LAGNA);
        if (fromMoon.present()) triggered.add(ReferencePoint.MOON);
        if (fromVenus.present()) triggered.add(ReferencePoint.VENUS);

        ManglikState state = triggered.isEmpty() ? ManglikState.NOT_MANGLIK : ManglikState.MANGLIK;

        return new ManglikStatus(
                state, fromLagna, fromMoon, fromVenus, triggered, List.of(), ruleVersion);
    }

    /**
     * Combine two per-person results. Compatibility is intentionally
     * conservative: only both-NOT_MANGLIK qualifies as {@code NEITHER_MANGLIK};
     * every other combination routes to {@code REQUIRES_DETAILED_REVIEW}.
     * The "Manglik-Manglik cancels" folk shortcut is explicitly disallowed
     * by the spec and is not applied here.
     */
    public ManglikCompatibility combine(ManglikStatus a, ManglikStatus b) {
        ManglikCompatibility.Compatibility compat;
        if (a.status() == ManglikState.NOT_MANGLIK && b.status() == ManglikState.NOT_MANGLIK) {
            compat = ManglikCompatibility.Compatibility.NEITHER_MANGLIK;
        } else {
            compat = ManglikCompatibility.Compatibility.REQUIRES_DETAILED_REVIEW;
        }
        return new ManglikCompatibility(a, b, compat, ruleVersion);
    }

    // ---- Whole-sign house math ----

    /** Sidereal longitude → Rashi (1..12), 0-based floor divide plus one. */
    private static int rashiOf(double longitude) {
        double norm = ((longitude % 360.0) + 360.0) % 360.0;
        int idx = (int) Math.floor(norm / 30.0);
        // Guard the 360-exact edge, defensively; % should already prevent 12.
        if (idx >= 12) idx = 11;
        return idx + 1;
    }

    /**
     * Whole-sign house of Mars from a reference sign, and whether that house
     * is one of the v1 trigger houses.
     */
    private ReferencePointResult check(int marsRashi, int refRashi) {
        int house = ((marsRashi - refRashi + 12) % 12) + 1;
        return new ReferencePointResult(house, triggerHouses.contains(house));
    }
}
