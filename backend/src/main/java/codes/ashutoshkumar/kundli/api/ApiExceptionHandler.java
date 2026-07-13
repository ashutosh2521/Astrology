package codes.ashutoshkumar.kundli.api;

import codes.ashutoshkumar.kundli.chart.EphemerisUnavailableException;
import codes.ashutoshkumar.kundli.chart.NotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("validation failed");
        return error(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * 502, not 500: the client's request was fine — the internal astronomy dependency
     * was unreachable or refused to confirm full precision. Never degrade silently.
     */
    @ExceptionHandler(EphemerisUnavailableException.class)
    public ResponseEntity<Map<String, String>> ephemeris(EphemerisUnavailableException e) {
        return error(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
