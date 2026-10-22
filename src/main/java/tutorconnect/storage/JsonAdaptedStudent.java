package tutorconnect.storage;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import tutorconnect.model.student.PaymentHistory;
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
        final List<Tag> studentTags = new ArrayList<>();
        for (JsonAdaptedTag tag : tags) {
            if (tag == null) {
                throw new IllegalValueException("Tags must not contain null entries.");
            }
            studentTags.add(tag.toModelType());
        }

        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName()));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Name.MESSAGE_CONSTRAINTS);
        }
        final Name modelName = new Name(name);

        if (phone == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName()));
        }
        if (!Phone.isValidPhone(phone)) {
            throw new IllegalValueException(Phone.MESSAGE_CONSTRAINTS);
        }
        final Phone modelPhone = new Phone(phone);

        if (email != null && !Email.isValidEmail(email)) {
            throw new IllegalValueException(Email.MESSAGE_CONSTRAINTS);
        }
        final Email modelEmail = email == null ? Email.unset() : new Email(email);

        if (address == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Address.class.getSimpleName()));
        }
        if (!Address.isValidAddress(address)) {
            throw new IllegalValueException(Address.MESSAGE_CONSTRAINTS);
        }
        final Address modelAddress = new Address(address);

        final Set<Tag> modelTags = new HashSet<>(studentTags);
        Optional<GuardianPhone> modelGuardianPhone = Optional.empty();
        if (guardianPhone != null) {
            if (!GuardianPhone.isValid(guardianPhone)) {
                throw new IllegalValueException(GuardianPhone.MESSAGE_CONSTRAINTS);
            }
            modelGuardianPhone = Optional.of(new GuardianPhone(guardianPhone));
        }

        Optional<HourlyRate> modelHourlyRate = Optional.empty();
        if (hourlyRate != null) {
            try {
                modelHourlyRate = Optional.of(new HourlyRate(new BigDecimal(hourlyRate)));
            } catch (IllegalArgumentException exception) {
                throw new IllegalValueException(HourlyRate.MESSAGE_CONSTRAINTS);
            }
        }

        final UUID modelId;
        try {
            // Stable migration IDs even when an older file has not yet been saved by this version.
            modelId = id == null ? UUID.nameUUIDFromBytes((modelName.fullName.toLowerCase(Locale.ROOT)
                    + "\u0000" + modelPhone.value).getBytes(StandardCharsets.UTF_8)) : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException("Invalid student ID");
        }
        StudentFields fields = new StudentFields(optionalFields);
        if (modelGuardianPhone.isPresent()) {
            fields = fields.with(GuardianPhone.FIELD, modelGuardianPhone.get());
        }
        if (modelHourlyRate.isPresent()) {
            fields = fields.with(HourlyRate.FIELD, modelHourlyRate.get());
        }
        try {
            // Validate the owner's complete value before publishing a loaded record.
            fields.get(WeeklySlotField.INSTANCE);
            fields.get(PaymentHistory.FIELD);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage());
        }
        return new Student(modelId, modelName, modelPhone, modelEmail, modelAddress, modelTags, fields);
    }

}
