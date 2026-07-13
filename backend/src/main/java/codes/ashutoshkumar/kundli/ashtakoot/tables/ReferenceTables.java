package codes.ashutoshkumar.kundli.ashtakoot.tables;

import codes.ashutoshkumar.kundli.ashtakoot.model.Gana;
import codes.ashutoshkumar.kundli.ashtakoot.model.Graha;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nadi;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import codes.ashutoshkumar.kundli.ashtakoot.model.Relation;
import codes.ashutoshkumar.kundli.ashtakoot.model.Varna;
import codes.ashutoshkumar.kundli.ashtakoot.model.VashyaGroup;
import codes.ashutoshkumar.kundli.ashtakoot.model.Yoni;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Loads all static Ashtakoot reference tables from classpath JSON at startup and
 * validates them for completeness. Per the spec, reference data is external JSON —
 * not hardcoded logic — so each table stays independently verifiable against a
 * source like Drik Panchang.
 *
 * <p>Validation is intentionally strict: a missing Rashi or Nakshatra key, or an
 * incomplete matrix, throws at load time rather than producing a silently wrong
 * score at match time. This mirrors the ephemeris service's "catch, don't silently
 * accept" stance.
 */
public final class ReferenceTables {

    private static final String BASE = "/ashtakoot/";

    // Varna
    private final Map<Rashi, Varna> varna;
    // Vashya
    private final Map<Rashi, VashyaGroup> vashyaGroup;
    private final Map<VashyaGroup, Map<VashyaGroup, Double>> vashyaMatrix;
    // Yoni
    private final Map<Nakshatra, Yoni> yoni;
    private final double yoniSame;
    private final double yoniDefault;
    private final Set<Set<Yoni>> yoniEnemies;
    // Graha Maitri
    private final Map<Rashi, Graha> lord;
    private final Map<Graha, Map<Graha, Relation>> friendship;
    private final Map<String, Double> grahaMaitriBands;
    // Gana
    private final Map<Nakshatra, Gana> gana;
    private final Map<Gana, Map<Gana, Double>> ganaMatrix;
    // Nadi
    private final Map<Nakshatra, Nadi> nadi;
    private final double nadiPoints;
    private final boolean nadiCancelSameRashiDiffNakshatra;
    private final boolean nadiCancelSameNakshatraDiffPada;
    // Bhakoot
    private final double bhakootPoints;
    private final Set<Set<Integer>> bhakootDoshaPairs;
    private final boolean bhakootCancelSameLord;
    private final boolean bhakootCancelLordsMutualFriends;

    private ReferenceTables(Builder b) {
        this.varna = b.varna;
        this.vashyaGroup = b.vashyaGroup;
        this.vashyaMatrix = b.vashyaMatrix;
        this.yoni = b.yoni;
        this.yoniSame = b.yoniSame;
        this.yoniDefault = b.yoniDefault;
        this.yoniEnemies = b.yoniEnemies;
        this.lord = b.lord;
        this.friendship = b.friendship;
        this.grahaMaitriBands = b.grahaMaitriBands;
        this.gana = b.gana;
        this.ganaMatrix = b.ganaMatrix;
        this.nadi = b.nadi;
        this.nadiPoints = b.nadiPoints;
        this.nadiCancelSameRashiDiffNakshatra = b.nadiCancelSameRashiDiffNakshatra;
        this.nadiCancelSameNakshatraDiffPada = b.nadiCancelSameNakshatraDiffPada;
        this.bhakootPoints = b.bhakootPoints;
        this.bhakootDoshaPairs = b.bhakootDoshaPairs;
        this.bhakootCancelSameLord = b.bhakootCancelSameLord;
        this.bhakootCancelLordsMutualFriends = b.bhakootCancelLordsMutualFriends;
    }

    // ---- Accessors ----

    public Varna varna(Rashi r) { return varna.get(r); }

    public VashyaGroup vashyaGroup(Rashi r) { return vashyaGroup.get(r); }

    public double vashyaScore(VashyaGroup boy, VashyaGroup girl) {
        return vashyaMatrix.get(boy).get(girl);
    }

    public Yoni yoni(Nakshatra n) { return yoni.get(n); }

    public double yoniScore(Yoni boy, Yoni girl) {
        if (boy == girl) {
            return yoniSame;
        }
        if (yoniEnemies.contains(Set.of(boy, girl))) {
            return 0.0;
        }
        return yoniDefault;
    }

    public Graha lord(Rashi r) { return lord.get(r); }

    public Relation relation(Graha from, Graha to) { return friendship.get(from).get(to); }

    public double grahaMaitriBand(String key) {
        Double v = grahaMaitriBands.get(key);
        if (v == null) {
            throw new IllegalStateException("Missing Graha Maitri band: " + key);
        }
        return v;
    }

    public Gana gana(Nakshatra n) { return gana.get(n); }

    public double ganaScore(Gana boy, Gana girl) { return ganaMatrix.get(boy).get(girl); }

    public Nadi nadi(Nakshatra n) { return nadi.get(n); }

    public double nadiPoints() { return nadiPoints; }

    public boolean nadiCancelSameRashiDiffNakshatra() { return nadiCancelSameRashiDiffNakshatra; }

    public boolean nadiCancelSameNakshatraDiffPada() { return nadiCancelSameNakshatraDiffPada; }

    public double bhakootPoints() { return bhakootPoints; }

    /** True if the pair of forward-counts between two Rashis is a Bhakoot dosha pair. */
    public boolean isBhakootDoshaPair(int countAB, int countBA) {
        // Dosha pairs (2/12, 5/9, 6/8) are always two distinct counts; equal counts
        // (same sign, 1/1) can never be a dosha and would break Set.of on duplicates.
        if (countAB == countBA) {
            return false;
        }
        return bhakootDoshaPairs.contains(Set.of(countAB, countBA));
    }

    public boolean bhakootCancelSameLord() { return bhakootCancelSameLord; }

    public boolean bhakootCancelLordsMutualFriends() { return bhakootCancelLordsMutualFriends; }

    // ---- Loading ----

    public static ReferenceTables load() {
        ObjectMapper om = new ObjectMapper();
        Builder b = new Builder();

        JsonNode varnaJson = read(om, "rashi_to_varna.json");
        b.varna = enumMap(varnaJson.get("data"), Rashi.class, Varna.class);

        JsonNode vashyaJson = read(om, "rashi_to_vashya_group.json");
        b.vashyaGroup = enumMap(vashyaJson.get("group"), Rashi.class, VashyaGroup.class);
        b.vashyaMatrix = doubleMatrix(vashyaJson.get("matrix"), VashyaGroup.class);

        JsonNode yoniJson = read(om, "nakshatra_to_yoni.json");
        b.yoni = enumMap(yoniJson.get("animal"), Nakshatra.class, Yoni.class);
        b.yoniSame = yoniJson.get("sameScore").asDouble();
        b.yoniDefault = yoniJson.get("defaultScore").asDouble();
        b.yoniEnemies = new HashSet<>();
        for (JsonNode pair : yoniJson.get("enemyPairs")) {
            b.yoniEnemies.add(Set.of(Yoni.valueOf(pair.get(0).asText()), Yoni.valueOf(pair.get(1).asText())));
        }

        JsonNode lordJson = read(om, "rashi_to_lord.json");
        b.lord = enumMap(lordJson.get("lord"), Rashi.class, Graha.class);
        b.friendship = relationMatrix(lordJson.get("friendship"));
        b.grahaMaitriBands = new java.util.HashMap<>();
        lordJson.get("bands").fields().forEachRemaining(
                e -> b.grahaMaitriBands.put(e.getKey(), e.getValue().asDouble()));

        JsonNode ganaJson = read(om, "nakshatra_to_gana.json");
        b.gana = enumMap(ganaJson.get("gana"), Nakshatra.class, Gana.class);
        b.ganaMatrix = doubleMatrix(ganaJson.get("matrix"), Gana.class);

        JsonNode nadiJson = read(om, "nakshatra_to_nadi.json");
        b.nadi = enumMap(nadiJson.get("nadi"), Nakshatra.class, Nadi.class);
        b.nadiPoints = nadiJson.get("points").asDouble();

        JsonNode nadiCancel = read(om, "nadi_cancellation_rules.json").get("cancellation");
        b.nadiCancelSameRashiDiffNakshatra = nadiCancel.get("cancelIfSameRashiDifferentNakshatra").asBoolean();
        b.nadiCancelSameNakshatraDiffPada = nadiCancel.get("cancelIfSameNakshatraDifferentPada").asBoolean();

        JsonNode bhakootJson = read(om, "bhakoot_rules.json");
        b.bhakootPoints = bhakootJson.get("points").asDouble();
        b.bhakootDoshaPairs = new HashSet<>();
        for (JsonNode pair : bhakootJson.get("doshaPairs")) {
            b.bhakootDoshaPairs.add(Set.of(pair.get(0).asInt(), pair.get(1).asInt()));
        }
        JsonNode bhakootCancel = bhakootJson.get("cancellation");
        b.bhakootCancelSameLord = bhakootCancel.get("cancelIfSameLord").asBoolean();
        b.bhakootCancelLordsMutualFriends = bhakootCancel.get("cancelIfLordsAreMutualFriends").asBoolean();

        ReferenceTables tables = new ReferenceTables(b);
        tables.validate();
        return tables;
    }

    private static JsonNode read(ObjectMapper om, String file) {
        try (InputStream in = ReferenceTables.class.getResourceAsStream(BASE + file)) {
            if (in == null) {
                throw new IllegalStateException("Reference table not found on classpath: " + BASE + file);
            }
            return om.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read reference table " + file, e);
        }
    }

    private static <K extends Enum<K>, V extends Enum<V>> Map<K, V> enumMap(
            JsonNode node, Class<K> keyType, Class<V> valType) {
        Map<K, V> map = new EnumMap<>(keyType);
        node.fields().forEachRemaining(e ->
                map.put(Enum.valueOf(keyType, e.getKey()), Enum.valueOf(valType, e.getValue().asText())));
        return map;
    }

    private static <K extends Enum<K>> Map<K, Map<K, Double>> doubleMatrix(JsonNode node, Class<K> keyType) {
        Map<K, Map<K, Double>> matrix = new EnumMap<>(keyType);
        node.fields().forEachRemaining(row -> {
            Map<K, Double> cols = new EnumMap<>(keyType);
            row.getValue().fields().forEachRemaining(c ->
                    cols.put(Enum.valueOf(keyType, c.getKey()), c.getValue().asDouble()));
            matrix.put(Enum.valueOf(keyType, row.getKey()), cols);
        });
        return matrix;
    }

    private static Map<Graha, Map<Graha, Relation>> relationMatrix(JsonNode node) {
        Map<Graha, Map<Graha, Relation>> matrix = new EnumMap<>(Graha.class);
        node.fields().forEachRemaining(row -> {
            Map<Graha, Relation> cols = new EnumMap<>(Graha.class);
            row.getValue().fields().forEachRemaining(c ->
                    cols.put(Graha.valueOf(c.getKey()), Relation.valueOf(c.getValue().asText())));
            matrix.put(Graha.valueOf(row.getKey()), cols);
        });
        return matrix;
    }

    /** Fail fast on any incomplete table — every domain key and matrix cell must exist. */
    private void validate() {
        for (Rashi r : Rashi.values()) {
            require(varna.get(r) != null, "varna missing " + r);
            require(vashyaGroup.get(r) != null, "vashyaGroup missing " + r);
            require(lord.get(r) != null, "lord missing " + r);
        }
        for (Nakshatra n : Nakshatra.values()) {
            require(yoni.get(n) != null, "yoni missing " + n);
            require(gana.get(n) != null, "gana missing " + n);
            require(nadi.get(n) != null, "nadi missing " + n);
        }
        for (VashyaGroup a : VashyaGroup.values()) {
            for (VashyaGroup c : VashyaGroup.values()) {
                require(vashyaMatrix.containsKey(a) && vashyaMatrix.get(a).containsKey(c),
                        "vashyaMatrix missing cell " + a + "->" + c);
            }
        }
        for (Gana a : Gana.values()) {
            for (Gana c : Gana.values()) {
                require(ganaMatrix.containsKey(a) && ganaMatrix.get(a).containsKey(c),
                        "ganaMatrix missing cell " + a + "->" + c);
            }
        }
        for (Graha a : Graha.values()) {
            for (Graha c : Graha.values()) {
                require(friendship.containsKey(a) && friendship.get(a).containsKey(c),
                        "friendship missing cell " + a + "->" + c);
            }
        }
    }

    private static void require(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException("Reference table validation failed: " + msg);
        }
    }

    private static final class Builder {
        Map<Rashi, Varna> varna;
        Map<Rashi, VashyaGroup> vashyaGroup;
        Map<VashyaGroup, Map<VashyaGroup, Double>> vashyaMatrix;
        Map<Nakshatra, Yoni> yoni;
        double yoniSame;
        double yoniDefault;
        Set<Set<Yoni>> yoniEnemies;
        Map<Rashi, Graha> lord;
        Map<Graha, Map<Graha, Relation>> friendship;
        Map<String, Double> grahaMaitriBands;
        Map<Nakshatra, Gana> gana;
        Map<Gana, Map<Gana, Double>> ganaMatrix;
        Map<Nakshatra, Nadi> nadi;
        double nadiPoints;
        boolean nadiCancelSameRashiDiffNakshatra;
        boolean nadiCancelSameNakshatraDiffPada;
        double bhakootPoints;
        Set<Set<Integer>> bhakootDoshaPairs;
        boolean bhakootCancelSameLord;
        boolean bhakootCancelLordsMutualFriends;
    }
}
