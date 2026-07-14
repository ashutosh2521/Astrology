package codes.ashutoshkumar.kundli.manglik;

/**
 * Couple-level Manglik result.
 *
 * <p>Compatibility value is intentionally conservative:
 * <ul>
 *   <li>{@link Compatibility#NEITHER_MANGLIK} when both partners are
 *       {@link ManglikState#NOT_MANGLIK}. This is the ONLY case v1 calls
 *       clean — everything else routes to detailed review.</li>
 *   <li>{@link Compatibility#REQUIRES_DETAILED_REVIEW} whenever either
 *       partner shows Manglik presence, regardless of whether the other
 *       is also Manglik. The spec forbids the "Manglik + Manglik cancel"
 *       shortcut, and v1 has no strength-graded logic to make a finer
 *       distinction.</li>
 * </ul>
 */
public record ManglikCompatibility(
        ManglikStatus personA,
        ManglikStatus personB,
        Compatibility compatibility,
        String ruleVersion
) {

    public enum Compatibility {
        /** Both partners cleared all three reference points. */
        NEITHER_MANGLIK,
        /** At least one partner shows Manglik presence — expert review needed. */
        REQUIRES_DETAILED_REVIEW
    }
}
