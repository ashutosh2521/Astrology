package codes.ashutoshkumar.kundli.manglik;

/**
 * The three whole-sign reference points from which Mars's house is counted
 * for the v1 Manglik check.
 *
 * <p>Fixed by the app's declared rule system ({@code manglik-v1.0} in
 * {@code manglik_rules.json}). Adding a new reference point (e.g. Jupiter,
 * Saturn) is a rule-version bump, not a code change to consumers.
 */
public enum ReferencePoint {
    /** Ascendant / Lagna. Uses the birth chart's Ascendant longitude. */
    LAGNA,
    /** Chandra Lagna. Mars house counted from the sign of the Moon. */
    MOON,
    /** Shukra Lagna. Mars house counted from the sign of Venus. */
    VENUS
}
