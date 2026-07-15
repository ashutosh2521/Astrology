package codes.ashutoshkumar.kundli.api;

import codes.ashutoshkumar.kundli.ashtakoot.model.Nakshatra;
import codes.ashutoshkumar.kundli.ashtakoot.model.Rashi;
import codes.ashutoshkumar.kundli.attributes.KundliAttributes;
import codes.ashutoshkumar.kundli.chart.BirthChart;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Request/response DTOs for the chart endpoints. */
public final class ChartDtos {

    private ChartDtos() {}

    public record CreateChartRequest(
            @NotBlank String label,
            @NotBlank String birthLocalDateTime,   // ISO local: 1990-05-15T14:53:00
            @NotBlank String timezone,             // IANA: Asia/Kolkata
            @Min(-90) @Max(90) double latitude,
            @Min(-180) @Max(180) double longitude,
            String placeName
    ) {}

    public record ChartResponse(
            long id,
            String label,
            String birthLocalDateTime,
            String timezone,
            double latitude,
            double longitude,
            String placeName,
            String utcInstant,
            String moonRashi,
            String moonNakshatra,
            int moonPada,
            List<String> warnings,
            String ayanamsa,
            String precision,
            String createdAt,
            /**
             * Descriptive kundli attributes (Gana, Nadi, Yoni, 7th house, …). Nullable:
             * {@code null} on responses that don't enrich the chart (e.g. the profile's
             * primary-chart pointer), so existing callers of {@link #from(BirthChart)}
             * are unaffected.
             */
            KundliAttributes attributes
    ) {
        /** Backward-compatible view with no derived attributes. */
        public static ChartResponse from(BirthChart c) {
            return from(c, null);
        }

        public static ChartResponse from(BirthChart c, KundliAttributes attributes) {
            return new ChartResponse(
                    c.getId(),
                    c.getLabel(),
                    c.getBirthLocalDateTime(),
                    c.getTimezone(),
                    c.getLatitude(),
                    c.getLongitude(),
                    c.getPlaceName(),
                    c.getUtcInstant(),
                    // Title-case Sanskrit — matches the frontend i18n lookups
                    // ("Mesha", "Purva Ashadha"), NOT the ALL-CAPS enum name.
                    Rashi.ofNumber(c.getMoonRashiNumber()).sanskritTitle(),
                    Nakshatra.ofNumber(c.getMoonNakshatraNumber()).sanskritTitle(),
                    c.getMoonPada(),
                    c.getWarnings() == null ? List.of() : List.of(c.getWarnings().split("\n")),
                    c.getAyanamsa(),
                    c.getPrecision(),
                    c.getCreatedAt(),
                    attributes);
        }
    }
}
