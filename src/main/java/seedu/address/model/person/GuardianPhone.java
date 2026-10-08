package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.AppUtil;

/** A guardian phone number stored in canonical Singapore international format. */
public final class GuardianPhone {

    public static final String MESSAGE_CONSTRAINTS =
            "Guardian phone must be a canonical Singapore number in the format +65XXXXXXXX.";
    public static final String VALIDATION_REGEX = "\\+65[689][0-9]{7}";

    public final String value;

    /** Constructs a canonical guardian phone number. */
    public GuardianPhone(String phone) {
        requireNonNull(phone);
        AppUtil.checkArgument(isValid(phone), MESSAGE_CONSTRAINTS);
        value = phone;
    }

    /** Returns whether {@code phone} is in canonical Singapore phone format. */
    public static boolean isValid(String phone) {
        return phone != null && phone.matches(VALIDATION_REGEX);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof GuardianPhone otherPhone && value.equals(otherPhone.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
