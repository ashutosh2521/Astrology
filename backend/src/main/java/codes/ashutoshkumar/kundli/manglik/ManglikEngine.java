package codes.ashutoshkumar.kundli.manglik;

import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Set<Integer> partialManglikCounts;
    private final Set<Integer> fullManglikCounts;
    /** Mars-sign → cancellation code. Non-empty for Mesha, Vrishchika, Makara, Karka. */
    private final Map<Integer, String> cancellationByMarsSign;

    ManglikEngine(String ruleVersion, Set<Integer> triggerHouses,
                  Set<Integer> partialManglikCounts, Set<Integer> fullManglikCounts,
                  Map<Integer, String> cancellationByMarsSign) {
        this.ruleVersion = ruleVersion;
        this.triggerHouses = Set.copyOf(triggerHouses);
        this.partialManglikCounts = Set.copyOf(partialManglikCounts);
        this.fullManglikCounts = Set.copyOf(fullManglikCounts);
        this.cancellationByMarsSign = Map.copyOf(cancellationByMarsSign);
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

        JsonNode grading = root.get("grading");
        Set<Integer> partialCounts = intSet(grading, "partialManglikWhenTriggeredCount");
        Set<Integer> fullCounts = intSet(grading, "manglikWhenTriggeredCount");
        // Defense: the three trigger-count buckets must partition {0..3}
        // (NOT_MANGLIK is implicitly 0). A rules-file bug that overlaps or
        // leaves a count unassigned would silently produce an inconsistent
        // grading; catch it at boot.
        Set<Integer> covered = new java.util.HashSet<>();
        covered.add(0);
        for (Integer n : partialCounts) {
            if (!covered.add(n)) {
                throw new IllegalStateException(
                        "manglik_rules.json grading: triggered count " + n
                        + " assigned to more than one bucket");
            }
        }
        for (Integer n : fullCounts) {
            if (!covered.add(n)) {
                throw new IllegalStateException(
                        "manglik_rules.json grading: triggered count " + n
                        + " assigned to more than one bucket");
            }
        }
        for (int n = 0; n <= 3; n++) {
            if (!covered.contains(n)) {
                throw new IllegalStateException(
                        "manglik_rules.json grading: triggered count " + n
                        + " not assigned to any bucket");
            }
        }

        // Cancellations (Mars-strength). Optional block; if missing, no cancellations
        // apply and the engine reverts to v1.1 behaviour.
        Map<Integer, String> cancellationByMarsSign = new HashMap<>();
        JsonNode cancellations = root.get("cancellations");
        if (cancellations != null && cancellations.hasNonNull("byMarsSign")) {
            for (JsonNode entry : cancellations.get("byMarsSign")) {
                String code = requireText(entry, "code");
                JsonNode signs = entry.get("signs");
                if (signs == null || !signs.isArray() || signs.isEmpty()) {
                    throw new IllegalStateException(
                            "manglik_rules.json cancellations." + code + ".signs must be a non-empty array");
                }
                for (JsonNode s : signs) {
                    // Resolve enum-name → Rashi ordinal (1..12). Fail-fast on unknown names.
                    Rashi rashi = Rashi.valueOf(s.asText());
                    String prior = cancellationByMarsSign.put(rashi.number(), code);
                    if (prior != null && !prior.equals(code)) {
                        throw new IllegalStateException(
                                "manglik_rules.json cancellations: sign " + rashi
                                + " assigned to both '" + prior + "' and '" + code + "'");
                    }
                }
            }
        }

        return new ManglikEngine(ruleVersion, triggerHouses, partialCounts, fullCounts,
                cancellationByMarsSign);
    }

    private static Set<Integer> intSet(JsonNode parent, String field) {
        if (parent == null || !parent.hasNonNull(field)) {
            throw new IllegalStateException("manglik_rules.json missing grading." + field);
        }
        JsonNode arr = parent.get(field);
        if (!arr.isArray()) {
            throw new IllegalStateException("manglik_rules.json grading." + field + " must be an array");
        }
        Set<Integer> out = new java.util.HashSet<>();
        for (JsonNode n : arr) {
            int v = n.asInt(-1);
            if (v < 0 || v > 3) {
                throw new IllegalStateException(
                        "manglik_rules.json grading." + field + " entry out of range 0..3: " + v);
            }
            out.add(v);
        }
        return out;
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

    /** For test introspection: which triggered-count values map to PARTIAL_MANGLIK. */
    Set<Integer> partialManglikCounts() {
        return partialManglikCounts;
    }

    /** For test introspection: which triggered-count values map to full MANGLIK. */
    Set<Integer> fullManglikCounts() {
        return fullManglikCounts;
    }

    /** For test introspection: Mars-sign → cancellation code map. */
    Map<Integer, String> cancellationByMarsSign() {
        return cancellationByMarsSign;
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

        // Mars-strength cancellations (v1.2): if Mars sits in own sign, exaltation
        // or debilitation, no Manglik regardless of triggered references. The raw
        // per-reference fields stay populated for transparency (the report can
        // show "would have triggered from Lagna house 8, but cancelled").
        String cancellationCode = cancellationByMarsSign.get(marsRashi);
        List<String> cancellations = cancellationCode == null ? List.of() : List.of(cancellationCode);

        ManglikState state = cancellationCode != null
                ? ManglikState.NOT_MANGLIK
                : gradeByTriggeredCount(triggered.size());

        return new ManglikStatus(
                state, fromLagna, fromMoon, fromVenus, triggered, cancellations, ruleVersion);
    }

    /**
     * Grade per-person state from the number of triggered reference points,
     * using the buckets loaded from {@code manglik_rules.json}. Every count
     * 0..3 lands in exactly one bucket (validated at construction time).
     */
    private ManglikState gradeByTriggeredCount(int count) {
        if (count == 0) return ManglikState.NOT_MANGLIK;
        if (partialManglikCounts.contains(count)) return ManglikState.PARTIAL_MANGLIK;
        if (fullManglikCounts.contains(count)) return ManglikState.MANGLIK;
        // Unreachable: partition validated at load.
        throw new IllegalStateException(
                "Unassigned triggered count in Manglik grading: " + count);
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
