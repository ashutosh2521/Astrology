package codes.ashutoshkumar.kundli.geocode;

/**
 * A single place found by the online geocoder, in the shape the frontend's
 * place-picker consumes. Mirrors the fields the bundled city list carries so
 * online and offline matches render and submit through the exact same path —
 * the user never sees latitude/longitude, and chart creation gets a resolved
 * coordinate + timezone either way.
 *
 * @param name      primary place name, e.g. "Bihta"
 * @param region    state (India) or country, e.g. "Bihar"
 * @param latitude  decimal degrees
 * @param longitude decimal degrees
 * @param timezone  IANA zone id used to convert the local birth time to UTC
 */
public record GeoResult(
        String name,
        String region,
        double latitude,
        double longitude,
        String timezone
) {}
