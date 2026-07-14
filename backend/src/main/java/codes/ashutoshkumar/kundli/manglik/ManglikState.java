package codes.ashutoshkumar.kundli.manglik;

/**
 * Per-person Manglik status.
 *
 * <p>All three values are actively emitted from {@code manglik-v1.1}:
 * <ul>
 *   <li>{@link #NOT_MANGLIK} — zero triggered reference points.</li>
 *   <li>{@link #PARTIAL_MANGLIK} — exactly one triggered reference point
 *       ("Anshik Manglik" / अंशिक मंगल दोष, per common Indian
 *       marriage-matching convention).</li>
 *   <li>{@link #MANGLIK} — two or three triggered reference points.</li>
 * </ul>
 *
 * <p>The specific 0 / 1 / 2+ grading is a v1.1 rule; historical results
 * tagged {@code manglik-v1.0} were emitted with only NOT_MANGLIK / MANGLIK
 * and remain readable as-is (their frozen ruleVersion on the MatchRecord
 * says exactly which grading produced them).
 */
public enum ManglikState {
    /** Zero triggered reference points. */
    NOT_MANGLIK,
    /** Exactly one triggered reference point — partial ("Anshik") Manglik. */
    PARTIAL_MANGLIK,
    /** Two or three triggered reference points — full Manglik. */
    MANGLIK
}
