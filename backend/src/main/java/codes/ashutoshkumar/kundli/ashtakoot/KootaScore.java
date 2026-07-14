package codes.ashutoshkumar.kundli.ashtakoot;

/**
 * The outcome of one koota, in the structured shape the brief asks for.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code code} — programmatic enum ({@link KootaCode}); the stable
 *       key downstream translations, PDF templates and audit tools use.</li>
 *   <li>{@code koota} — display name in English ("Varna", "Graha Maitri").
 *       Kept alongside {@code code} so the wire JSON is human-readable
 *       and older Angular components that already consume this string
 *       do not break.</li>
 *   <li>{@code personAValue}, {@code personBValue} — the specific
 *       Sanskrit value the koota was evaluated on for each person
 *       (Varna tier for Varna, Nakshatra Yoni for Yoni, Nadi for Nadi,
 *       Rashi lord for Graha Maitri, Rashi-to-Rashi count for Bhakoot,
 *       Nakshatra count remainder for Tara). Title-case; the exact form
 *       the brief's example uses.</li>
 *   <li>{@code points}, {@code maxPoints} — scored points and the koota's
 *       ceiling; kept as {@code double} because Vashya, Tara and Graha
 *       Maitri can award half-points.</li>
 *   <li>{@code doshaPresent} — {@code true} only when this koota is a
 *       dosha condition and it fired (Nadi same-Nadi, Bhakoot dosha pair).
 *       The engine still reports whether that dosha was cancelled via the
 *       separate {@link DoshaStatus} block on the result — the two views
 *       are complementary.</li>
 *   <li>{@code ruleApplied} — enumerated code for the specific rule
 *       branch that produced the score (e.g.
 *       {@code SAME_MOON_SIGN_LORD}, {@code MOD9_BOTH_AUSPICIOUS},
 *       {@code SWORN_ENEMY_YONI_PAIR}). Frontend translates this code
 *       into a Hindi/English explanation; the code is stable, the text
 *       is not.</li>
 *   <li>{@code detail} — free-text human-readable explanation. Retained
 *       for the hover-reveal UI in {@code koota-bars.component.ts}; a
 *       future refactor may derive it from {@code ruleApplied} + values
 *       and drop it, but that's a UI change.</li>
 * </ul>
 *
 * <p>Kept as an unrestricted record: no builder, no annotations, so the
 * engine's scorers can construct one in a single expression. Adding a
 * new field here means updating every scorer in one PR, which is the
 * signal we want when the shape changes.
 */
public record KootaScore(
        KootaCode code,
        String koota,
        String personAValue,
        String personBValue,
        double points,
        double maxPoints,
        boolean doshaPresent,
        String ruleApplied,
        String detail
) {}
