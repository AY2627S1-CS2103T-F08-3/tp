package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

/**
 * Represents a person's optional remark. Any non-null string is valid.
 */
public class Remark {

    public final String value;

    /**
     * Constructs a {@code Remark}, which may be empty.
     */
    public Remark(String remark) {
        requireNonNull(remark);
        value = remark;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof Remark otherRemark && value.equals(otherRemark.value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
