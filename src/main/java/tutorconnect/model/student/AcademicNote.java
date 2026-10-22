package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.commons.util.AppUtil;
import tutorconnect.commons.util.StudentText;

/**
 * A student's single freeform academic note, such as "weak in calculus, short attention span".
 * Case and punctuation are kept as typed; equality is exact.
 */
public final class AcademicNote {

    public static final int MAX_LENGTH = 500;
    public static final String MESSAGE_CONSTRAINTS =
            "Error: Invalid note. Use 1-500 characters with no control characters or line breaks.";

    private static final String UNSET_DISPLAY = "—";

    public static final StudentField<AcademicNote> FIELD = new StudentField<>() {
        @Override
        public String key() {
            return "academicNote";
        }

        @Override
        public AcademicNote decode(JsonNode value) {
            AppUtil.checkArgument(value.isTextual(), MESSAGE_CONSTRAINTS);
            return new AcademicNote(value.asText());
        }

        @Override
        public JsonNode encode(AcademicNote value) {
            return TextNode.valueOf(value.value);
        }
    };

    /** The normalized note text. */
    public final String value;

    /**
     * Constructs an {@code AcademicNote}, normalizing Unicode and spacing first.
     *
     * @throws IllegalArgumentException if the note is empty, too long, or contains control characters.
     */
    public AcademicNote(String note) {
        requireNonNull(note);
        String normalized;
        try {
            normalized = StudentText.normalize(note);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(MESSAGE_CONSTRAINTS, e);
        }
        int length = StudentText.length(normalized);
        AppUtil.checkArgument(length >= 1 && length <= MAX_LENGTH, MESSAGE_CONSTRAINTS);
        value = normalized;
    }

    /** Returns the note to show, or an em dash if it is unset or the stored value is unreadable. */
    public static String display(StudentFields fields) {
        try {
            return fields.get(FIELD).map(AcademicNote::toString).orElse(UNSET_DISPLAY);
        } catch (IllegalArgumentException e) {
            return UNSET_DISPLAY;
        }
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof AcademicNote otherNote && value.equals(otherNote.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
