package codes.ashutoshkumar.kundli.ashtakoot.model;

/**
 * The 12 Rashis (zodiac signs), ordered from Mesha (Aries) at sidereal 0°.
 *
 * <p>This is an identity type only. Every astrological mapping keyed off a Rashi
 * — Varna, Vashya group, ruling Graha — lives in static JSON reference data, not
 * here, so those tables stay independently verifiable against Drik Panchang.
 */
public enum Rashi {
    MESHA(1, "Aries"),
    VRISHABHA(2, "Taurus"),
    MITHUNA(3, "Gemini"),
    KARKA(4, "Cancer"),
    SIMHA(5, "Leo"),
    KANYA(6, "Virgo"),
    TULA(7, "Libra"),
    VRISHCHIKA(8, "Scorpio"),
    DHANU(9, "Sagittarius"),
    MAKARA(10, "Capricorn"),
    KUMBHA(11, "Aquarius"),
    MEENA(12, "Pisces");

    private final int number;
    private final String english;

    Rashi(int number, String english) {
        this.number = number;
        this.english = english;
    }

    /** 1-based ordinal (1 = Mesha … 12 = Meena). */
    public int number() {
        return number;
    }

    public String english() {
        return english;
    }

    public static Rashi ofNumber(int number) {
        if (number < 1 || number > 12) {
            throw new IllegalArgumentException("Rashi number out of range: " + number);
        }
        return values()[number - 1];
    }

    /**
     * Count of Rashis from this sign to {@code other}, inclusive of both ends,
     * moving forward (1..12). Used by Bhakoot, which keys off this cyclic distance.
     */
    public int countTo(Rashi other) {
        int diff = other.number - this.number;
        if (diff < 0) {
            diff += 12;
        }
        return diff + 1;
    }
}
