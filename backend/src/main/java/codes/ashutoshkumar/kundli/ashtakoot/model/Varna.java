package codes.ashutoshkumar.kundli.ashtakoot.model;

/**
 * The 4 Varna tiers, ranked. Varna koota awards its point when the boy's tier is
 * greater than or equal to the girl's, so each carries a numeric {@link #rank()}.
 */
public enum Varna {
    SHUDRA(1),
    VAISHYA(2),
    KSHATRIYA(3),
    BRAHMIN(4);

    private final int rank;

    Varna(int rank) {
        this.rank = rank;
    }

    /** Higher = more senior tier (Brahmin = 4 … Shudra = 1). */
    public int rank() {
        return rank;
    }
}
