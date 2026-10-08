package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import seedu.address.commons.util.StudentText;

/**
 * Represents a Person's name in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Error: Invalid name. Use 1-70 letters with spaces, apostrophes, hyphens, or full stops.";

    // Validation follows NFKC normalization; at least one Unicode letter is required separately.
    public static final String VALIDATION_REGEX = "[\\p{L}\\p{M} '\u2019.\\-]+";

    public final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = StudentText.normalize(name);
    }

    /**
     * Returns true if a given string is a valid name.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        try {
            String value = StudentText.normalize(test);
            return StudentText.length(value) >= 1 && StudentText.length(value) <= 70
                    && value.matches(VALIDATION_REGEX) && value.codePoints().anyMatch(Character::isLetter);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
