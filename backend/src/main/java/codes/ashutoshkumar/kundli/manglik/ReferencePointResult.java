package codes.ashutoshkumar.kundli.manglik;

/**
 * Result of the Manglik check from a single reference point.
 *
 * <p>{@code marsHouse} is always computed (1..12) so a UI can display
 * "Mars in 3rd from Moon (safe)" even when the rule did not fire.
 * {@code present} is true only when {@code marsHouse} is one of the six
 * trigger houses declared in {@code manglik_rules.json}.
 *
 * @param marsHouse whole-sign house index, 1..12
 * @param present   true iff {@code marsHouse} ∈ {1, 2, 4, 7, 8, 12} per v1 rules
 */
public record ReferencePointResult(int marsHouse, boolean present) {
    public ReferencePointResult {
        if (marsHouse < 1 || marsHouse > 12) {
            throw new IllegalArgumentException("marsHouse out of range 1..12: " + marsHouse);
        }
    }
}
