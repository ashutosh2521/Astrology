package codes.ashutoshkumar.kundli.ashtakoot;

/**
 * Status of a dosha (Nadi or Bhakoot) that has cancellation exceptions.
 *
 * <p>The spec is explicit that raw point totals without cancellation handling
 * produce "technically-scored but practically-wrong verdicts", so a dosha is
 * reported with both whether it is {@code present} and whether it is {@code cancelled},
 * plus the {@code reason} for the cancellation decision (traceable to the chosen
 * Drik Panchang convention).
 */
public record DoshaStatus(String name, boolean present, boolean cancelled, String reason) {

    /** True when the dosha is present and NOT cancelled — i.e. it actually applies. */
    public boolean isEffective() {
        return present && !cancelled;
    }
}
