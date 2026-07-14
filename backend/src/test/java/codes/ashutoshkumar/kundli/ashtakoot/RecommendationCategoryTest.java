package codes.ashutoshkumar.kundli.ashtakoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Locks the boundary semantics of the three-way category derivation.
 *
 * <p>Brief §15 default: "Below 18 = कमजोर, 18 to 24 = मध्यम, Above 24 = अच्छा."
 * Implemented as: total &lt; 18 → WEAK; ≥ 18 and &lt; 25 → MODERATE;
 * ≥ 25 → STRONG. Half-point totals (24.5, 17.5) fall on the expected side.
 */
class RecommendationCategoryTest {

    @ParameterizedTest(name = "total={0} → {1}")
    @CsvSource({
            "0.0,   WEAK",
            "17.99, WEAK",
            "17.5,  WEAK",
            "18.0,  MODERATE",
            "18.001,MODERATE",
            "24.5,  MODERATE",
            "24.999,MODERATE",
            "25.0,  STRONG",
            "30.5,  STRONG",
            "36.0,  STRONG",
    })
    void categorizeAgainstDefaultThresholds(double total, String expected) {
        assertEquals(RecommendationCategory.valueOf(expected),
                RecommendationCategory.forTotal(total, 18.0, 25.0));
    }

    @Test
    void moderateMinNotBelowStrongMinIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RecommendationCategory.forTotal(20.0, 25.0, 25.0),
                "strongMin must be strictly greater than moderateMin");
        assertThrows(IllegalArgumentException.class,
                () -> RecommendationCategory.forTotal(20.0, 25.0, 20.0));
    }

    @Test
    void thresholdsAreConfigurable() {
        // Stricter thresholds: WEAK < 20; MODERATE < 28; STRONG ≥ 28.
        assertEquals(RecommendationCategory.WEAK,
                RecommendationCategory.forTotal(19.5, 20.0, 28.0));
        assertEquals(RecommendationCategory.MODERATE,
                RecommendationCategory.forTotal(22.0, 20.0, 28.0));
        assertEquals(RecommendationCategory.STRONG,
                RecommendationCategory.forTotal(28.0, 20.0, 28.0));
    }
}
