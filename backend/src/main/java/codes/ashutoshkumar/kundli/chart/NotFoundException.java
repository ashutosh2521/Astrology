package codes.ashutoshkumar.kundli.chart;

/** Requested entity does not exist; mapped to 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
