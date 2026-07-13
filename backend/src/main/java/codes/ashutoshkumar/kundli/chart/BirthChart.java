package codes.ashutoshkumar.kundli.chart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * A person's birth chart: the raw inputs AND the cached ephemeris output.
 *
 * <p>Charts are computed once (profile creation/edit) and reused across matches —
 * the ephemeris service is never called at match time. The full response is kept in
 * {@link #chartJson} so later features (Lagna/Mangal Dosha) and post-upgrade audits
 * can read stored charts without recomputation.
 *
 * <p>Deliberately portable column types (ISO strings, doubles, TEXT JSON) — no
 * SQLite- or Postgres-specific features, keeping the Postgres upgrade path a
 * dialect swap (locked-in decision; see root README).
 */
@Entity
@Table(name = "birth_charts")
public class BirthChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display label — typically the person's name. */
    @Column(nullable = false)
    private String label;

    // --- Raw birth inputs (what the user asserted) ---
    /** Local birth date-time, ISO-8601, exactly as entered. */
    @Column(nullable = false)
    private String birthLocalDateTime;

    /** IANA timezone id used for local→UTC resolution (e.g. Asia/Kolkata). */
    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String placeName;

    // --- Resolution + computed placement (what the system derived) ---
    /** Resolved UTC instant actually sent to the ephemeris service, ISO-8601. */
    @Column(nullable = false)
    private String utcInstant;

    @Column(nullable = false)
    private int moonRashiNumber;

    @Column(nullable = false)
    private int moonNakshatraNumber;

    @Column(nullable = false)
    private int moonPada;

    private double moonDegreesToRashiBoundary;

    private double moonDegreesToNakshatraBoundary;

    /** Newline-joined accuracy warnings (boundary proximity, historical timezone). */
    private String warnings;

    // --- Audit trail ---
    @Column(nullable = false)
    private String ayanamsa;

    /** Ephemeris precision as confirmed per-response (e.g. SWISS_EPHEMERIS_FULL). */
    @Column(nullable = false)
    private String precision;

    /** Full ephemeris response, verbatim. */
    @Lob
    @Column(nullable = false)
    private String chartJson;

    @Column(nullable = false)
    private String createdAt;

    protected BirthChart() {
        // JPA
    }

    public BirthChart(String label, String birthLocalDateTime, String timezone,
                      double latitude, double longitude, String placeName,
                      String utcInstant, int moonRashiNumber, int moonNakshatraNumber,
                      int moonPada, double moonDegreesToRashiBoundary,
                      double moonDegreesToNakshatraBoundary, String warnings,
                      String ayanamsa, String precision, String chartJson, String createdAt) {
        this.label = label;
        this.birthLocalDateTime = birthLocalDateTime;
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeName = placeName;
        this.utcInstant = utcInstant;
        this.moonRashiNumber = moonRashiNumber;
        this.moonNakshatraNumber = moonNakshatraNumber;
        this.moonPada = moonPada;
        this.moonDegreesToRashiBoundary = moonDegreesToRashiBoundary;
        this.moonDegreesToNakshatraBoundary = moonDegreesToNakshatraBoundary;
        this.warnings = warnings;
        this.ayanamsa = ayanamsa;
        this.precision = precision;
        this.chartJson = chartJson;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public String getBirthLocalDateTime() { return birthLocalDateTime; }
    public String getTimezone() { return timezone; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPlaceName() { return placeName; }
    public String getUtcInstant() { return utcInstant; }
    public int getMoonRashiNumber() { return moonRashiNumber; }
    public int getMoonNakshatraNumber() { return moonNakshatraNumber; }
    public int getMoonPada() { return moonPada; }
    public double getMoonDegreesToRashiBoundary() { return moonDegreesToRashiBoundary; }
    public double getMoonDegreesToNakshatraBoundary() { return moonDegreesToNakshatraBoundary; }
    public String getWarnings() { return warnings; }
    public String getAyanamsa() { return ayanamsa; }
    public String getPrecision() { return precision; }
    public String getChartJson() { return chartJson; }
    public String getCreatedAt() { return createdAt; }
}
