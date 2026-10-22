package tutorconnect.model.student;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extension contract for the optional-field owners. Implementations validate and encode their own type.
 * Keys include guardianPhone, educationLevel, subject, hourlyRate, weeklySlot, and payments.
 */
public interface StudentField<T> {
    String key();

    T decode(JsonNode value);

    JsonNode encode(T value);
}
