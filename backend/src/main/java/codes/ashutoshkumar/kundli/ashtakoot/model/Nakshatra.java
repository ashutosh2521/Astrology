package codes.ashutoshkumar.kundli.ashtakoot.model;

/**
 * The 27 Nakshatras, ordered from Ashwini at sidereal 0°.
 *
 * <p>Identity type only. Nakshatra → Yoni, → Gana, → Nadi mappings live in static
 * JSON reference data, independently verifiable against Drik Panchang.
 */
public enum Nakshatra {
    ASHWINI(1, "Ashwini"),
    BHARANI(2, "Bharani"),
    KRITTIKA(3, "Krittika"),
    ROHINI(4, "Rohini"),
    MRIGASHIRA(5, "Mrigashira"),
    ARDRA(6, "Ardra"),
    PUNARVASU(7, "Punarvasu"),
    PUSHYA(8, "Pushya"),
    ASHLESHA(9, "Ashlesha"),
    MAGHA(10, "Magha"),
    PURVA_PHALGUNI(11, "Purva Phalguni"),
    UTTARA_PHALGUNI(12, "Uttara Phalguni"),
    HASTA(13, "Hasta"),
    CHITRA(14, "Chitra"),
    SWATI(15, "Swati"),
    VISHAKHA(16, "Vishakha"),
    ANURADHA(17, "Anuradha"),
    JYESHTHA(18, "Jyeshtha"),
    MULA(19, "Mula"),
    PURVA_ASHADHA(20, "Purva Ashadha"),
    UTTARA_ASHADHA(21, "Uttara Ashadha"),
    SHRAVANA(22, "Shravana"),
    DHANISHTA(23, "Dhanishta"),
    SHATABHISHA(24, "Shatabhisha"),
    PURVA_BHADRAPADA(25, "Purva Bhadrapada"),
    UTTARA_BHADRAPADA(26, "Uttara Bhadrapada"),
    REVATI(27, "Revati");

    private final int number;
    private final String sanskritTitle;

    Nakshatra(int number, String sanskritTitle) {
        this.number = number;
        this.sanskritTitle = sanskritTitle;
    }

    /** 1-based ordinal (1 = Ashwini … 27 = Revati). */
    public int number() {
        return number;
    }

    /**
     * Title-case Sanskrit name — the display form the UI expects
     * ("Ashwini", "Purva Ashadha", "Uttara Bhadrapada"). Never the
     * ALL-CAPS enum {@link #name()}, which is an internal identifier
     * — and never the underscore form ("PURVA_ASHADHA"), which the
     * frontend Hindi/English lookups cannot resolve.
     */
    public String sanskritTitle() {
        return sanskritTitle;
    }

    public static Nakshatra ofNumber(int number) {
        if (number < 1 || number > 27) {
            throw new IllegalArgumentException("Nakshatra number out of range: " + number);
        }
        return values()[number - 1];
    }

    /**
     * Forward count from this Nakshatra to {@code other}, inclusive (1..27).
     * Tara koota keys off this count taken in each direction.
     */
    public int countTo(Nakshatra other) {
        int diff = other.number - this.number;
        if (diff < 0) {
            diff += 27;
        }
        return diff + 1;
    }
}
