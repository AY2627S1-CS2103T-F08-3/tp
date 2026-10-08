package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import seedu.address.commons.util.AppUtil;
import seedu.address.commons.util.StudentText;

/**
 * The single subject a student is taught. The display case is kept as entered, but equality ignores case
 * and repeated spaces.
 */
public final class Subject {

    public static final String MESSAGE_CONSTRAINTS =
            "Error: Invalid subject. Use 2-50 letters, numbers, spaces, or the symbols & / - ' ( ).";

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[\\p{L}\\p{Nd} &/'()\\-]+");

    public static final StudentField<Subject> FIELD = new StudentField<>() {
        @Override
        public String key() {
            return "subject";
        }

        @Override
        public Subject decode(JsonNode value) {
            AppUtil.checkArgument(value.isTextual(), MESSAGE_CONSTRAINTS);
            return new Subject(value.asText());
        }

        @Override
        public JsonNode encode(Subject value) {
            return TextNode.valueOf(value.value);
        }
    };

    /** The normalized subject with its original letter case. */
    public final String value;

    /**
     * Constructs a {@code Subject}, normalizing Unicode and spacing first.
     *
     * @throws IllegalArgumentException if the subject is not valid.
     */
    public Subject(String subject) {
        requireNonNull(subject);
        String normalized;
        try {
            normalized = StudentText.normalize(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(MESSAGE_CONSTRAINTS, e);
        }
        int length = StudentText.length(normalized);
        AppUtil.checkArgument(length >= MIN_LENGTH && length <= MAX_LENGTH
                && ALLOWED_CHARACTERS.matcher(normalized).matches()
                && normalized.codePoints().anyMatch(Character::isLetter), MESSAGE_CONSTRAINTS);
        value = normalized;
    }

    private String comparisonKey() {
        return value.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other == this
                || other instanceof Subject otherSubject && comparisonKey().equals(otherSubject.comparisonKey());
    }

    @Override
    public int hashCode() {
        return comparisonKey().hashCode();
    }
}
