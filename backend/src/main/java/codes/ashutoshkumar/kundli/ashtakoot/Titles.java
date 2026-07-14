package codes.ashutoshkumar.kundli.ashtakoot;

/**
 * Title-case display for single-word Sanskrit enum values (Varna,
 * VashyaGroup, Yoni, Gana, Nadi, Graha, Relation). Turns {@code MADHYA}
 * into {@code "Madhya"} — the exact form the brief's example koota result
 * uses ({@code personAValue: "Madhya"}), and the form Angular's i18n
 * lookups already key on.
 *
 * <p>Rashi and Nakshatra have their own {@code sanskritTitle()} accessors
 * because Nakshatra names are multi-word ({@code PURVA_ASHADHA →
 * "Purva Ashadha"}). Every other Ashtakoot value enum is single-word, so
 * a shared uppercase-first helper is enough.
 */
public final class Titles {

    private Titles() {}

    /** Return the enum's constant name with only the first letter upper-case. */
    public static String titleCase(Enum<?> value) {
        String name = value.name();
        if (name.isEmpty()) {
            return name;
        }
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
