package codes.ashutoshkumar.kundli.manglik;

/**
 * Per-person Manglik status.
 *
 * <p>Version 1 emits only {@link #NOT_MANGLIK} and {@link #MANGLIK}. The
 * {@link #PARTIAL_MANGLIK} value is declared in the enum so that a future
 * v1.1 can distinguish strength-graded results without breaking the
 * wire contract for existing clients — but v1 must not emit it, per
 * the spec's "do not invent cancellation rules / do not automatically
 * declare Manglik-Manglik safe" constraints.
 */
public enum ManglikState {
    /** No trigger house from any of Lagna, Moon or Venus. */
    NOT_MANGLIK,
    /** At least one reference point places Mars in a trigger house. */
    MANGLIK,
    /**
     * Reserved for v1.1. A future strength-graded rule set may emit this
     * when Manglik presence is weak (e.g. only from Venus, or Mars is
     * exalted/own-sign). V1 must never emit it.
     */
    PARTIAL_MANGLIK
}
