package codes.ashutoshkumar.kundli.attributes;

import codes.ashutoshkumar.kundli.ashtakoot.model.Graha;
import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import codes.ashutoshkumar.kundli.ashtakoot.tables.ReferenceTables;
import codes.ashutoshkumar.kundli.attributes.KundliAttributes.Occupant;
import codes.ashutoshkumar.kundli.attributes.KundliAttributes.SeventhHouse;
import codes.ashutoshkumar.kundli.chart.BirthChart;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Derives read-only descriptive {@link KundliAttributes} for a stored chart.
 *
 * <p>Everything is computed from data already persisted at chart-creation time — the
 * Moon's Rashi/Nakshatra numbers, the Ascendant longitude, and the full ephemeris
 * JSON blob — so this never calls the ephemeris service and works for every existing
 * chart. The Moon-derived attributes (Gana, Nadi, Yoni, Varna, Vashya, sign lord)
 * reuse the exact same {@link ReferenceTables} the match engine trusts, so they can
 * never disagree with a match result computed from the same chart.
 */
@Service
public class KundliAttributesService {

    /**
     * Natural benefics, by ephemeris graha name. Sun, Mars, Saturn, Rahu and Ketu are
     * the natural malefics (everything not in this set). This is the standard textbook
     * classification used for a first-pass house read; the conditional cases (Mercury
     * or the Moon turning malefic by association, a waning Moon) are out of scope and
     * called out in {@link KundliAttributes.SeventhHouse}.
     */
    private static final Set<String> NATURAL_BENEFICS = Set.of("Jupiter", "Venus", "Mercury", "Moon");

    private static final int SIGNS = 12;
    private static final double SIGN_SPAN = 30.0;

    private final ReferenceTables tables;
    private final ObjectMapper mapper;

    public KundliAttributesService(ReferenceTables tables, ObjectMapper mapper) {
        this.tables = tables;
        this.mapper = mapper;
    }

    public KundliAttributes derive(BirthChart chart) {
        Rashi moonRashi = Rashi.ofNumber(chart.getMoonRashiNumber());
        Nakshatra moonNakshatra = Nakshatra.ofNumber(chart.getMoonNakshatraNumber());

        return new KundliAttributes(
                tables.gana(moonNakshatra).name(),
                tables.nadi(moonNakshatra).name(),
                tables.yoni(moonNakshatra).name(),
                tables.varna(moonRashi).name(),
                tables.vashyaGroup(moonRashi).name(),
                tables.lord(moonRashi).name(),
                seventhHouse(chart));
    }

    /**
     * Whole-sign 7th house from the Lagna. Returns {@code null} for legacy charts
     * stored before the Ascendant was captured — the frontend renders the rest of the
     * attributes and simply omits the house block.
     */
    private SeventhHouse seventhHouse(BirthChart chart) {
        Double ascendantLongitude = chart.getAscendantLongitude();
        if (ascendantLongitude == null) {
            return null;
        }
        int lagnaIndex = (int) Math.floor((ascendantLongitude % 360.0) / SIGN_SPAN);   // 0..11
        int seventhIndex = (lagnaIndex + 6) % SIGNS;
        Rashi lagnaSign = Rashi.ofNumber(lagnaIndex + 1);
        Rashi seventhSign = Rashi.ofNumber(seventhIndex + 1);
        Graha seventhLord = tables.lord(seventhSign);

        List<Occupant> occupants = occupantsOf(chart, seventhIndex + 1);
        return new SeventhHouse(
                lagnaSign.sanskritTitle(),
                seventhSign.sanskritTitle(),
                seventhLord.name(),
                occupants,
                assess(occupants));
    }

    /** Grahas whose sidereal sign (from the stored chart JSON) is {@code signNumber} (1..12). */
    private List<Occupant> occupantsOf(BirthChart chart, int signNumber) {
        List<Occupant> occupants = new ArrayList<>();
        JsonNode root;
        try {
            root = mapper.readTree(chart.getChartJson());
        } catch (Exception e) {
            // Chart JSON is written full-precision and validated at creation; an
            // unreadable blob means we can't attribute occupants, so report none
            // rather than guess. The house sign and lord are still meaningful.
            return occupants;
        }
        for (JsonNode graha : root.path("grahas")) {
            if (graha.path("rashi").path("number").asInt() == signNumber) {
                String name = graha.path("name").asText();
                occupants.add(new Occupant(name, NATURAL_BENEFICS.contains(name)));
            }
        }
        return occupants;
    }

    /**
     * Coarse, transparent verdict: a 7th house with no natural malefic in it reads
     * FAVOURABLE; malefics softened by a benefic together read MIXED; only malefics
     * reads NEEDS_ATTENTION. An empty house is FAVOURABLE (nothing afflicting it).
     */
    private static String assess(List<Occupant> occupants) {
        boolean hasMalefic = occupants.stream().anyMatch(o -> !o.benefic());
        boolean hasBenefic = occupants.stream().anyMatch(Occupant::benefic);
        if (!hasMalefic) {
            return "FAVOURABLE";
        }
        return hasBenefic ? "MIXED" : "NEEDS_ATTENTION";
    }
}
