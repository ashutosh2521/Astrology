package codes.ashutoshkumar.kundli.manglik;

import java.util.List;

/**
 * Per-person Manglik result, in the structured shape the app spec asks
 * every result to expose (a stable code + the exact reference point and
 * house that triggered it + a rule-version audit tag).
 *
 * <p>Explanations (English / Hindi) are intentionally NOT part of this
 * record — the engine emits codes only; translation is a UI concern.
 * The Angular i18n layer keys off {@code status} + {@code triggeredReferences}
 * to render Hindi/English text.
 *
 * @param status               overall per-person state (NOT_MANGLIK / MANGLIK)
 * @param fromLagna            Mars house from Ascendant + whether the rule fired
 * @param fromMoon             Mars house from Moon sign + whether the rule fired
 * @param fromVenus            Mars house from Venus sign + whether the rule fired
 * @param triggeredReferences  the subset of reference points that fired the rule;
 *                             empty iff {@code status == NOT_MANGLIK}
 * @param cancellations        list of cancellation-rule codes that fired;
 *                             empty in v1 (advanced cancellations are v1.1)
 * @param ruleVersion          the exact rule-set version that produced this result
 */
public record ManglikStatus(
        ManglikState status,
        ReferencePointResult fromLagna,
        ReferencePointResult fromMoon,
        ReferencePointResult fromVenus,
        List<ReferencePoint> triggeredReferences,
        List<String> cancellations,
        String ruleVersion
) {
    public ManglikStatus {
        // Defense in depth: triggeredReferences must be consistent with status.
        // An engine bug that reports MANGLIK with no triggered references (or
        // vice versa) would flow all the way into stored match results;
        // failing loudly at construction time is cheaper than a silently
        // wrong verdict.
        boolean anyTriggered =
                fromLagna.present() || fromMoon.present() || fromVenus.present();
        if (status == ManglikState.MANGLIK && !anyTriggered) {
            throw new IllegalStateException(
                    "status=MANGLIK requires at least one reference point to have present=true");
        }
        if (status == ManglikState.NOT_MANGLIK && anyTriggered) {
            throw new IllegalStateException(
                    "status=NOT_MANGLIK requires all reference points to have present=false");
        }
        triggeredReferences = List.copyOf(triggeredReferences);
        cancellations = List.copyOf(cancellations);
    }
}
