package codes.ashutoshkumar.kundli.ashtakoot;

/**
 * The outcome of one koota.
 *
 * <p>{@code points} is a double because several kootas (Vashya, Graha Maitri) award
 * fractional partial credit. {@code detail} is a human-readable explanation of how
 * the score was reached — surfaced in the UI and invaluable when cross-checking a
 * result against a reference like Drik Panchang.
 */
public record KootaScore(String koota, double points, double maxPoints, String detail) {}
