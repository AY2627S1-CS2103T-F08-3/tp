package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Immutable optional-field envelope. Owners supply typed codecs through StudentField.
 * Unknown fields are preserved so one owner's changes cannot erase another owner's data.
 */
public final class StudentFields {
    private final Map<String, JsonNode> values;

    public StudentFields() {
        this(Map.of());
    }

    /** Restores an independent envelope, treating missing or null values as unset. */
    public StudentFields(Map<String, JsonNode> values) {
        requireNonNull(values);
        this.values = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            requireNonNull(key);
            if (value != null && !value.isNull()) {
                this.values.put(key, value.deepCopy());
            }
        });
    }

    /** Returns a detached JSON snapshot for the storage adapter. */
    public Map<String, JsonNode> toStorage() {
        Map<String, JsonNode> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, value.deepCopy()));
        return copy;
    }

    public <T> Optional<T> get(StudentField<T> field) {
        JsonNode value = values.get(field.key());
        return value == null ? Optional.empty() : Optional.of(field.decode(value.deepCopy()));
    }

    /** Produces a copy containing the owner's validated value, preserving all unrelated fields. */
    public <T> StudentFields with(StudentField<T> field, T value) {
        Map<String, JsonNode> copy = toStorage();
        JsonNode encoded = requireNonNull(field.encode(requireNonNull(value)));
        field.decode(encoded.deepCopy());
        copy.put(field.key(), encoded);
        return new StudentFields(copy);
    }

    /** Display fallback until each field owner supplies richer formatting. Unset fields use an em dash. */
    public String display(String key) {
        JsonNode value = values.get(key);
        return value == null ? "—" : value.isValueNode() ? value.asText() : value.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof StudentFields fields && values.equals(fields.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
