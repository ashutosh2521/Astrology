package codes.ashutoshkumar.kundli.ashtakoot;

/**
 * The three preliminary-match categories the Mother-mode UI renders.
 *
 * <p>Category is derived from the Ashtakoot total against configurable
 * thresholds ({@code kundli.recommendation.moderate-minimum} and
 * {@code kundli.recommendation.strong-minimum}), NOT from a hard-coded
 * band inside the engine. Every threshold movement is a config change,
 * never a code change.
 *
 * <p>Categories match the exact Hindi phrases the brief mandates:
 * <ul>
 *   <li>{@link #WEAK}     — कमजोर प्रारंभिक मिलान</li>
 *   <li>{@link #MODERATE} — मध्यम मिलान — विस्तृत जांच आवश्यक</li>
 *   <li>{@link #STRONG}   — अच्छा प्रारंभिक मिलान</li>
 * </ul>
 *
 * <p>This is the total-score verdict only. Nadi/Bhakoot/Manglik doshas
 * are surfaced separately on the result and must NOT downgrade the
 * category by themselves (spec: "Do not reject based only on total
 * score. Show Nadi, Bhakoot and Manglik separately.").
 */
public enum RecommendationCategory {
    WEAK,
    MODERATE,
    STRONG;

    /**
     * Category for a total score against two configurable thresholds.
     *
     * @param total           the eight-koota total (0..36)
     * @param moderateMin     scores below this are WEAK; ≥ this and < strongMin are MODERATE
     * @param strongMin       scores ≥ this are STRONG
     */
    public static RecommendationCategory forTotal(double total, double moderateMin, double strongMin) {
        if (strongMin <= moderateMin) {
            throw new IllegalArgumentException(
                    "strongMin must be greater than moderateMin: " + strongMin + " <= " + moderateMin);
        }
        if (total < moderateMin) return WEAK;
        if (total < strongMin) return MODERATE;
        return STRONG;
    }
}
