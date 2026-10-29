package tutorconnect.storage;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import tutorconnect.commons.exceptions.IllegalValueException;
import tutorconnect.model.student.Address;
import tutorconnect.model.student.Email;
import tutorconnect.model.student.GuardianPhone;
import tutorconnect.model.student.HourlyRate;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.model.tag.Tag;

/**
 * Jackson-friendly version of {@link Student}.
 */
class JsonAdaptedStudent {

    public static final String MISSING_FIELD_MESSAGE_FORMAT = "Student's %s field is missing!";

    private final String name;
    private final String id;
    @JsonIgnore
    private final Map<String, JsonNode> optionalFields = new LinkedHashMap<>();
    private final String phone;
    private final String email;
    private final String address;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String guardianPhone;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String hourlyRate;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();

    /**
     * Constructs a {@code JsonAdaptedStudent} with the given student details.
     */
    public JsonAdaptedStudent(String name, String phone, String email, String address, List<JsonAdaptedTag> tags) {
        this(null, name, phone, email, address, tags, null, null);
    }

    /** Constructs a {@code JsonAdaptedStudent} with all persisted student details. */
    @JsonCreator
    public JsonAdaptedStudent(@JsonProperty("id") String id,
            @JsonProperty("name") String name, @JsonProperty("phone") String phone,
            @JsonProperty("email") String email, @JsonProperty("address") String address,
            @JsonProperty("tags") List<JsonAdaptedTag> tags,
            @JsonProperty("guardianPhone") String guardianPhone,
            @JsonProperty("hourlyRate") String hourlyRate) {
        this.name = name;
        this.id = id;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.guardianPhone = guardianPhone;
        this.hourlyRate = hourlyRate;
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    /** Compatibility constructor for old records and existing adapter clients. */
    public JsonAdaptedStudent(String name, String phone, String email, String address, List<JsonAdaptedTag> tags,
            String guardianPhone, String hourlyRate) {
        this(null, name, phone, email, address, tags, guardianPhone, hourlyRate);
    }

    /**
     * Converts a given {@code Student} into this class for Jackson use.
     */
    public JsonAdaptedStudent(Student source) {
        id = source.getId().toString();
        optionalFields.putAll(source.getStudentFields().toStorage());
        // These two keys use the explicit legacy JSON properties, never duplicate any-getter entries.
        optionalFields.remove(GuardianPhone.FIELD.key());
        optionalFields.remove(HourlyRate.FIELD.key());
        name = source.getName().fullName;
        phone = source.getPhone().value;
        email = source.getEmail().value.isEmpty() ? null : source.getEmail().value;
        address = source.getAddress().value;
        guardianPhone = source.getGuardianPhone().map(GuardianPhone::toString).orElse(null);
        hourlyRate = source.getHourlyRate().map(rate -> rate.value.toPlainString()).orElse(null);
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
    }

    @JsonAnySetter
    public void readOptionalField(String key, JsonNode value) {
        optionalFields.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, JsonNode> writeOptionalFields() {
        return new StudentFields(optionalFields).toStorage();
    }

    /**
     * Converts this Jackson-friendly adapted student object into the model's {@code Student} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted student.
     */
    public Student toModelType() throws IllegalValueException {
        final Set<Tag> modelTags = toModelTags();
        final Name modelName = toModelName();
        final Phone modelPhone = toModelPhone();
        final Email modelEmail = toModelEmail();
        final Address modelAddress = toModelAddress();
        final StudentFields fields = toModelFields();
        final UUID modelId = toModelId(modelName, modelPhone);
        return new Student(modelId, modelName, modelPhone, modelEmail, modelAddress, modelTags, fields);
    }

    private Set<Tag> toModelTags() throws IllegalValueException {
        final Set<Tag> modelTags = new HashSet<>();
        for (JsonAdaptedTag tag : tags) {
            if (tag == null) {
                throw new IllegalValueException("Tags must not contain null entries.");
            }
            modelTags.add(tag.toModelType());
        }
        return modelTags;
    }

    private Name toModelName() throws IllegalValueException {
        requirePresent(name, Name.class);
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Name.MESSAGE_CONSTRAINTS);
        }
        return new Name(name);
    }

    private Phone toModelPhone() throws IllegalValueException {
        requirePresent(phone, Phone.class);
        if (!Phone.isValidPhone(phone)) {
            throw new IllegalValueException(Phone.MESSAGE_CONSTRAINTS);
        }
        return new Phone(phone);
    }

    private Email toModelEmail() throws IllegalValueException {
        if (email == null) {
            return Email.unset();
        }
        if (!Email.isValidEmail(email)) {
            throw new IllegalValueException(Email.MESSAGE_CONSTRAINTS);
        }
        return new Email(email);
    }

    private Address toModelAddress() throws IllegalValueException {
        requirePresent(address, Address.class);
        if (!Address.isValidAddress(address)) {
            throw new IllegalValueException(Address.MESSAGE_CONSTRAINTS);
        }
        return new Address(address);
    }

    private static void requirePresent(String value, Class<?> fieldType) throws IllegalValueException {
        if (value == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, fieldType.getSimpleName()));
        }
    }

    /** Returns the optional fields, with the guardian phone and hourly rate checked and the weekly slot validated. */
    private StudentFields toModelFields() throws IllegalValueException {
        StudentFields fields = new StudentFields(optionalFields);
        if (guardianPhone != null) {
            if (!GuardianPhone.isValid(guardianPhone)) {
                throw new IllegalValueException(GuardianPhone.MESSAGE_CONSTRAINTS);
            }
            fields = fields.with(GuardianPhone.FIELD, new GuardianPhone(guardianPhone));
        }
        if (hourlyRate != null) {
            try {
                fields = fields.with(HourlyRate.FIELD, new HourlyRate(new BigDecimal(hourlyRate)));
            } catch (IllegalArgumentException exception) {
                throw new IllegalValueException(HourlyRate.MESSAGE_CONSTRAINTS);
            }
        }
        try {
            // Validate the owner's complete value before publishing a loaded record.
            fields.get(WeeklySlotField.INSTANCE);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage());
        }
        return fields;
    }

    /** Restores a canonical UUID, or derives a stable identity for legacy records without an ID. */
    private UUID toModelId(Name modelName, Phone modelPhone) throws IllegalValueException {
        if (id == null) {
            return UUID.nameUUIDFromBytes((modelName.fullName.toLowerCase(Locale.ROOT)
                    + "\u0000" + modelPhone.value).getBytes(StandardCharsets.UTF_8));
        }
        try {
            UUID parsed = UUID.fromString(id);
            // UUID.fromString also accepts abbreviated components; persisted identities must be complete.
            if (!parsed.toString().equalsIgnoreCase(id)) {
                throw new IllegalArgumentException("Noncanonical student ID");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException("Invalid student ID");
        }
    }

}
