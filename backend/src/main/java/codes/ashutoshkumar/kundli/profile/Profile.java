package codes.ashutoshkumar.kundli.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The app's one primary profile.
 *
 * <p>This is a single-family app: my mother uses it to screen prospective
 * matches for me. There is exactly ONE primary partner (Ashutosh), and
 * every new match runs against that primary. Enforced by a fixed
 * {@link #KEY} — the entity has at most one row.
 *
 * <p>Kept intentionally narrow: the primary profile is a *pointer* to a
 * BirthChart, not a duplicate of its fields. That means fixing a birth-
 * detail typo on the primary chart is a chart edit, not a two-step
 * update. When the pointer is null (first run), Mother-mode UI shows
 * the "set up primary profile" wizard.
 */
@Entity
@Table(name = "profile")
public class Profile {

    /**
     * Fixed key for the single primary-profile row. Using a string PK
     * makes the "at most one row" invariant a database-level constraint
     * — a duplicate insert would fail the unique key, not require an
     * application-level guard.
     */
    public static final String KEY = "primary";

    @Id
    @Column(nullable = false)
    private String id;

    /** BirthChart id this profile points at; nullable until first setup. */
    private Long primaryChartId;

    @Column(nullable = false)
    private String updatedAt;

    protected Profile() {
        // JPA
    }

    public Profile(Long primaryChartId, String updatedAt) {
        this.id = KEY;
        this.primaryChartId = primaryChartId;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public Long getPrimaryChartId() { return primaryChartId; }
    public String getUpdatedAt() { return updatedAt; }

    void setPrimaryChartId(Long primaryChartId) { this.primaryChartId = primaryChartId; }
    void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
