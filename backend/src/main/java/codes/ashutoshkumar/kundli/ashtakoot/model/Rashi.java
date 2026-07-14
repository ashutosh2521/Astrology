package codes.ashutoshkumar.kundli.ashtakoot.model;

/**
 * The 12 Rashis (zodiac signs), ordered from Mesha (Aries) at sidereal 0°.
 *
 * <p>This is an identity type only. Every astrological mapping keyed off a Rashi
 * — Varna, Vashya group, ruling Graha — lives in static JSON reference data, not
 * here, so those tables stay independently verifiable against Drik Panchang.
 */
public enum Rashi {
    MESHA(1, "Aries", "Mesha"),
    VRISHABHA(2, "Taurus", "Vrishabha"),
    MITHUNA(3, "Gemini", "Mithuna"),
    KARKA(4, "Cancer", "Karka"),
    SIMHA(5, "Leo", "Simha"),
    KANYA(6, "Virgo", "Kanya"),
    TULA(7, "Libra", "Tula"),
    VRISHCHIKA(8, "Scorpio", "Vrishchika"),
    DHANU(9, "Sagittarius", "Dhanu"),
    MAKARA(10, "Capricorn", "Makara"),
    KUMBHA(11, "Aquarius", "Kumbha"),
    MEENA(12, "Pisces", "Meena");

    private final int number;
    private final String english;
    private final String sanskritTitle;

    Rashi(int number, String english, String sanskritTitle) {
        this.number = number;
        this.english = english;
        this.sanskritTitle = sanskritTitle;
    }

    /** 1-based ordinal (1 = Mesha … 12 = Meena). */
    public int number() {
        return number;
    }

    public String english() {
        return english;
    }

    /**
     * Title-case Sanskrit name — the display form the UI expects
     * ("Mesha", "Vrishchika"). Never the ALL-CAPS enum {@link #name()},
     * which is an internal identifier.
     */
    public String sanskritTitle() {
        return sanskritTitle;
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
