package codes.ashutoshkumar.kundli.ashtakoot.model;

/**
 * The 27 Nakshatras, ordered from Ashwini at sidereal 0°.
 *
 * <p>Identity type only. Nakshatra → Yoni, → Gana, → Nadi mappings live in static
 * JSON reference data, independently verifiable against Drik Panchang.
 */
public enum Nakshatra {
    ASHWINI(1), BHARANI(2), KRITTIKA(3), ROHINI(4), MRIGASHIRA(5), ARDRA(6),
    PUNARVASU(7), PUSHYA(8), ASHLESHA(9), MAGHA(10), PURVA_PHALGUNI(11),
    UTTARA_PHALGUNI(12), HASTA(13), CHITRA(14), SWATI(15), VISHAKHA(16),
    ANURADHA(17), JYESHTHA(18), MULA(19), PURVA_ASHADHA(20), UTTARA_ASHADHA(21),
    SHRAVANA(22), DHANISHTA(23), SHATABHISHA(24), PURVA_BHADRAPADA(25),
    UTTARA_BHADRAPADA(26), REVATI(27);

    private final int number;

    Nakshatra(int number) {
        this.number = number;
    }

    /** 1-based ordinal (1 = Ashwini … 27 = Revati). */
    public int number() {
        return number;
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
