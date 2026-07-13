package codes.ashutoshkumar.kundli.chart;

/**
 * The ephemeris service is unreachable, errored, or could not confirm full precision.
 * Mapped to 502 — the caller's request was fine; the astronomy backend was not.
 */
public class EphemerisUnavailableException extends RuntimeException {
    public EphemerisUnavailableException(String message) {
        super(message);
    }

    public EphemerisUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
