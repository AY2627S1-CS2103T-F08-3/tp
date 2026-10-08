package seedu.address.model.person;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extension contract for the optional-field owners. Implementations validate and encode their own type.
 * Keys are guardianPhone, educationLevel, subject, hourlyRate, and weeklySlot.
 */
public interface StudentField<T> {
    String key();

    T decode(JsonNode value);

    JsonNode encode(T value);
}
