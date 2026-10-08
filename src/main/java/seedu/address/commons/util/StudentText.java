package seedu.address.commons.util;

import static java.util.Objects.requireNonNull;

import java.text.Normalizer;

/** Shared normalization for student fields. Never call trim before checking prohibited characters. */
public final class StudentText {
    private StudentText() {}

    /** Controls, format characters, and line/paragraph separators are not student data. */
    public static boolean hasProhibitedCharacters(String value) {
        requireNonNull(value);
        return value.codePoints().anyMatch(cp -> Character.isISOControl(cp)
                || Character.getType(cp) == Character.FORMAT
                || Character.getType(cp) == Character.LINE_SEPARATOR
                || Character.getType(cp) == Character.PARAGRAPH_SEPARATOR
                || Character.getType(cp) == Character.SURROGATE);
    }

    /** NFKC, Unicode-space collapse, and trim; rejects controls before and after normalization. */
    public static String normalize(String value) {
        requireNonNull(value);
        if (hasProhibitedCharacters(value)) {
            throw new IllegalArgumentException("Prohibited control or line break");
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        if (hasProhibitedCharacters(normalized)) {
            throw new IllegalArgumentException("Prohibited control or line break");
        }
        return normalized.replaceAll("\\p{Zs}+", " ").strip();
    }

    /** Length is measured in Unicode code points, not UTF-16 code units. */
    public static int length(String value) {
        return value.codePointCount(0, value.length());
    }
}
