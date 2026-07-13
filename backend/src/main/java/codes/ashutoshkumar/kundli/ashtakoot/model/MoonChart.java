package codes.ashutoshkumar.kundli.ashtakoot.model;

/**
 * The minimal per-person input Ashtakoot matching needs: the Moon's Rashi,
 * Nakshatra and Pada. This is exactly the subset of the ephemeris service's chart
 * output that the 8 kootas consume — matching never needs the full planetary chart.
 *
 * @param rashi     Moon's sign
 * @param nakshatra Moon's Nakshatra
 * @param pada      1..4, the quarter of the Nakshatra (some cancellation rules use it)
 */
public record MoonChart(Rashi rashi, Nakshatra nakshatra, int pada) {
    public MoonChart {
        if (pada < 1 || pada > 4) {
            throw new IllegalArgumentException("pada out of range 1..4: " + pada);
        }
    }
}
