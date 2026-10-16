package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.commons.util.AppUtil;
import tutorconnect.commons.util.StudentText;

/** A student's current education level, always held in canonical form such as {@code Secondary 4}. */
public final class EducationLevel {

    public static final String MESSAGE_CONSTRAINTS =
            "Error: Invalid level. Use Primary 1-6, Secondary 1-5, or JC 1-2.";

    /** Canonical names and the documented aliases only; spaces inside a level are optional. */
    private static final Pattern INPUT_PATTERN = Pattern.compile(
            "(?:(primary|p) ?([1-6])|(secondary|sec|s) ?([1-5])|(jc) ?([1-2]))", Pattern.CASE_INSENSITIVE);

    public static final StudentField<EducationLevel> FIELD = new StudentField<>() {
        @Override
        public String key() {
            return "educationLevel";
        }

        @Override
        public EducationLevel decode(JsonNode value) {
            AppUtil.checkArgument(value.isTextual(), MESSAGE_CONSTRAINTS);
            return new EducationLevel(value.asText());
        }

        @Override
        public JsonNode encode(EducationLevel value) {
            return TextNode.valueOf(value.value);
        }
    };

    private static final String UNSET_DISPLAY = "—";

    /** The canonical level name. */
    public final String value;

    /**
     * Constructs an {@code EducationLevel} from a canonical name or alias, ignoring case.
     *
     * @throws IllegalArgumentException if the text is not a supported level.
     */
    public EducationLevel(String level) {
        requireNonNull(level);
        value = canonicalize(level);
    }

    private static String canonicalize(String level) {
        String normalized;
        try {
            normalized = StudentText.normalize(level);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(MESSAGE_CONSTRAINTS, e);
        }
        Matcher matcher = INPUT_PATTERN.matcher(normalized);
        AppUtil.checkArgument(matcher.matches(), MESSAGE_CONSTRAINTS);
        if (matcher.group(1) != null) {
            return "Primary " + matcher.group(2);
        }
        if (matcher.group(3) != null) {
            return "Secondary " + matcher.group(4);
        }
        return "JC " + matcher.group(6);
    }

    /**
     * Returns the text to show for a student's level: the canonical name, or an em dash if it is unset.
     * A stored value that is not a supported level is shown as unset, as the {@code level} command treats it.
     */
    public static String display(StudentFields fields) {
        try {
            return fields.get(FIELD).map(level -> level.value).orElse(UNSET_DISPLAY);
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
        return other == this || other instanceof EducationLevel otherLevel && value.equals(otherLevel.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
