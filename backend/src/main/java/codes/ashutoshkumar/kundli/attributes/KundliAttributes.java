package codes.ashutoshkumar.kundli.attributes;

import java.util.List;

/**
 * Read-only descriptive attributes of a single kundli, derived from already-stored
 * chart data — no ephemeris recompute. Everything here is a pure function of the
 * Moon's Rashi/Nakshatra (via the same verified reference tables the match engine
 * uses) or of the stored Ascendant + graha positions.
 *
 * <p>All fields carry stable UPPER_CASE enum codes (e.g. {@code DEVA}, {@code AADI},
 * {@code JUPITER}), not display text: translation to English/Hindi is the frontend's
 * job, exactly like the Ashtakoot koota results. A {@code null} {@link #seventhHouse()}
 * means the chart predates Ascendant storage and its house analysis can't be derived.
 */
public record KundliAttributes(
        String gana,          // DEVA / MANUSHYA / RAKSHASA          (from Moon Nakshatra)
        String nadi,          // AADI / MADHYA / ANTYA               (from Moon Nakshatra)
        String yoni,          // HORSE / ELEPHANT / …                (from Moon Nakshatra)
        String varna,         // BRAHMIN / KSHATRIYA / VAISHYA / SHUDRA (from Moon Rashi)
        String vashya,        // CHATUSHPADA / MANAVA / …            (from Moon Rashi)
        String moonSignLord,  // SUN / MOON / … ruling the Moon Rashi
        SeventhHouse seventhHouse) {

    /**
     * The 7th house — the house of marriage and partnership — read from the Lagna
     * using the whole-sign house system, with a deliberately simple, transparent
     * verdict. This is a first-pass indicator only: it looks at which planets sit in
     * the 7th and their natural benefic/malefic nature. It does NOT consider aspects
     * onto the house, the dignity of the 7th lord, combustion, or dashas — a full
     * reading needs all of those and an astrologer. The {@code assessment} is
     * intentionally coarse so the UI can present it honestly.
     */
    public record SeventhHouse(
            String lagnaSign,        // Ascendant Rashi (title-case Sanskrit, e.g. "Tula")
            String sign,             // 7th-house Rashi, whole-sign from Lagna
            String lord,             // Graha ruling the 7th sign (UPPER_CASE code)
            List<Occupant> occupants,
            String assessment) {}    // FAVOURABLE / MIXED / NEEDS_ATTENTION

    /** A planet sitting in the 7th house, with its natural benefic/malefic nature. */
    public record Occupant(
            String graha,            // "Sun" … "Ketu" (ephemeris name)
            boolean benefic) {}
}
