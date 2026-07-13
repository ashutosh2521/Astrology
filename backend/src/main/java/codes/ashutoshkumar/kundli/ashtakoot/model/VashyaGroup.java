package codes.ashutoshkumar.kundli.ashtakoot.model;

/**
 * The 5 Vashya groups a Rashi belongs to. Vashya koota scores from a 5×5 matrix
 * (partial credit possible) held in JSON reference data.
 *
 * <p>Note: classically some Rashis split across two groups by half-sign (e.g. Dhanu,
 * Makara). The reference data uses a whole-sign assignment; the exact assignment and
 * the matrix values are marked for verification against Drik Panchang.
 */
public enum VashyaGroup {
    CHATUSHPADA,   // quadruped
    MANAVA,        // human / biped (Dwipada)
    JALACHARA,     // aquatic
    VANACHARA,     // wild animal
    KEETA          // insect / reptile
}
