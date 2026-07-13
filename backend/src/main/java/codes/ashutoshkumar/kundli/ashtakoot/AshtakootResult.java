package codes.ashutoshkumar.kundli.ashtakoot;

import java.util.List;

/**
 * The full result of an Ashtakoot match: the eight per-koota scores, the total out
 * of 36, the dosha statuses (with cancellation applied), and a summary verdict.
 */
public record AshtakootResult(
        List<KootaScore> kootas,
        double totalPoints,
        double maxPoints,
        List<DoshaStatus> doshas,
        String verdict
) {}
