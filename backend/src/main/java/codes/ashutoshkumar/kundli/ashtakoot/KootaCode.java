package codes.ashutoshkumar.kundli.ashtakoot;

/**
 * Programmatic identifier for each of the 8 kootas.
 *
 * <p>The engine emits {@code code} on every {@link KootaScore} so consumers
 * (the API DTO layer, the Angular i18n service, the eventual PDF report)
 * key their translations off a stable enum rather than parsing the display
 * string. Renaming a koota label ("Graha Maitri" ↔ "Grah Maitri") must be
 * a UI-only change and must never break a stored result's lookup.
 *
 * <p>Ordinal order matches the traditional Ashtakoota presentation
 * (Varna first … Nadi last) and equals the engine's evaluation order.
 */
public enum KootaCode {
    VARNA,
    VASHYA,
    TARA,
    YONI,
    GRAHA_MAITRI,
    GANA,
    BHAKOOT,
    NADI
}
