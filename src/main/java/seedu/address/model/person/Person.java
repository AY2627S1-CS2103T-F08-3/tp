package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Represents a Person in the address book.
 * Guarantees: required details are present and validated, optional details are explicitly unset or validated,
 * and the object is immutable.
 */
public class Person {

    // Identity fields
    private final Name name;
    private final Phone phone;
    private final Email email;

    // Data fields
    private final Address address;
    private final Set<Tag> tags = new HashSet<>();
    private final Optional<GuardianPhone> guardianPhone;
    private final Optional<HourlyRate> hourlyRate;

    /**
     * Creates a person without guardian phone or hourly rate details.
     */
    public Person(Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        this(name, phone, email, address, tags, Optional.empty(), Optional.empty());
    }

    /** Creates a person with optional guardian phone and hourly rate fields. */
    public Person(Name name, Phone phone, Email email, Address address, Set<Tag> tags,
            Optional<GuardianPhone> guardianPhone, Optional<HourlyRate> hourlyRate) {
        requireAllNonNull(name, phone, email, address, tags, guardianPhone, hourlyRate);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.tags.addAll(tags);
        this.guardianPhone = guardianPhone;
        this.hourlyRate = hourlyRate;
    }

    public Name getName() {
        return name;
    }

    public Phone getPhone() {
        return phone;
    }

    public Email getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public Optional<GuardianPhone> getGuardianPhone() {
        return guardianPhone;
    }

    public Optional<HourlyRate> getHourlyRate() {
        return hourlyRate;
    }

    /**
     * Returns true if both persons have the same name.
     * This defines a weaker notion of equality between two persons.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && otherPerson.getName().equals(getName());
    }

    /**
     * Returns true if both persons have the same identity and data fields.
     * This defines a stronger notion of equality between two persons.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Person otherPerson)) {
            return false;
        }

        return name.equals(otherPerson.name)
                && phone.equals(otherPerson.phone)
                && email.equals(otherPerson.email)
                && address.equals(otherPerson.address)
                && tags.equals(otherPerson.tags)
                && guardianPhone.equals(otherPerson.guardianPhone)
                && hourlyRate.equals(otherPerson.hourlyRate);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, address, tags, guardianPhone, hourlyRate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("address", address)
                .add("tags", tags)
                .add("guardianPhone", guardianPhone)
                .add("hourlyRate", hourlyRate)
                .toString();
    }

}
