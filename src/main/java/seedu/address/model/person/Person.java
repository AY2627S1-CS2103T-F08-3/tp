package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Represents a Person in the address book.
 * Guarantees: required details are present and validated, optional details are explicitly unset or validated,
 * and the object is immutable.
 */
public class Person {

    private final UUID id;
    private final StudentFields studentFields;

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
        this(UUID.randomUUID(), name, phone, email, address, tags, new StudentFields());
    }

    /** Creates a student with no optional fields or legacy contact metadata. */
    public Person(Name name, Phone phone, Address address) {
        this(name, phone, Email.unset(), address, Set.of());
    }

    /** Restores a complete immutable student; field updates must retain the ID and unrelated fields. */
    public Person(UUID id, Name name, Phone phone, Email email, Address address, Set<Tag> tags,
            StudentFields studentFields) {
        requireAllNonNull(name, phone, email, address, tags);
        requireAllNonNull(id, studentFields);
        this.id = id;
        this.studentFields = studentFields;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.tags.addAll(tags);
        this.guardianPhone = guardianPhone;
        this.hourlyRate = hourlyRate;
    }

    public UUID getId() {
        return id;
    }

    public StudentFields getStudentFields() {
        return studentFields;
    }

    /** Returns an updated snapshot, without changing internal identity or optional fields. */
    public Person withDetails(Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        return new Person(id, name, phone, email, address, tags, studentFields);
    }

    public Person withStudentFields(StudentFields fields) {
        return new Person(id, name, phone, email, address, tags, fields);
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
     * Returns true if both students have equal normalized names (ignoring case) and canonical phones.
     * This defines a weaker notion of equality between two persons.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && otherPerson.getName().fullName.equalsIgnoreCase(getName().fullName)
                && otherPerson.getPhone().equals(getPhone());
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
                && studentFields.equals(otherPerson.studentFields);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, address, tags, guardianPhone, hourlyRate);
        return Objects.hash(name, phone, email, address, tags, studentFields);
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
