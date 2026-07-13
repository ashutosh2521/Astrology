package codes.ashutoshkumar.kundli.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * A saved match between two birth charts. Stores the full AshtakootResult as JSON
 * plus the rules version used, so a stored verdict remains auditable after the
 * reference tables are corrected (e.g. when a provisional cell is verified against
 * Drik Panchang and changes).
 */
@Entity
@Table(name = "matches")
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long boyChartId;

    @Column(nullable = false)
    private Long girlChartId;

    @Column(nullable = false)
    private double totalPoints;

    @Column(nullable = false)
    private double maxPoints;

    @Column(nullable = false)
    private String verdict;

    /** Full AshtakootResult (kootas, doshas, details), serialized. */
    @Lob
    @Column(nullable = false)
    private String resultJson;

    /** Version of the reference-table set that produced this result. */
    @Column(nullable = false)
    private String rulesVersion;

    @Column(nullable = false)
    private String createdAt;

    protected MatchRecord() {
        // JPA
    }

    public MatchRecord(Long boyChartId, Long girlChartId, double totalPoints, double maxPoints,
                       String verdict, String resultJson, String rulesVersion, String createdAt) {
        this.boyChartId = boyChartId;
        this.girlChartId = girlChartId;
        this.totalPoints = totalPoints;
        this.maxPoints = maxPoints;
        this.verdict = verdict;
        this.resultJson = resultJson;
        this.rulesVersion = rulesVersion;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getBoyChartId() { return boyChartId; }
    public Long getGirlChartId() { return girlChartId; }
    public double getTotalPoints() { return totalPoints; }
    public double getMaxPoints() { return maxPoints; }
    public String getVerdict() { return verdict; }
    public String getResultJson() { return resultJson; }
    public String getRulesVersion() { return rulesVersion; }
    public String getCreatedAt() { return createdAt; }
}
