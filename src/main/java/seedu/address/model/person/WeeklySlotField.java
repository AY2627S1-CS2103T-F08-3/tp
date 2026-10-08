package seedu.address.model.person;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * F08's typed codec for the shared optional-field envelope. A partial weekly pair is never a valid value.
 */
public final class WeeklySlotField implements StudentField<WeeklySlot> {
    public static final WeeklySlotField INSTANCE = new WeeklySlotField();

    private WeeklySlotField() {}

    @Override
    public String key() {
        return "weeklySlot";
    }

    @Override
    public WeeklySlot decode(JsonNode value) {
        if (!value.isObject() || !value.hasNonNull("day") || !value.hasNonNull("time")
                || !value.get("day").isTextual() || !value.get("time").isTextual()) {
            throw new IllegalArgumentException("Weekly lesson must contain both day and time.");
        }
        return new WeeklySlot(WeeklySlot.parseDay(value.get("day").asText()),
                WeeklySlot.parseTime(value.get("time").asText()));
    }

    @Override
    public JsonNode encode(WeeklySlot value) {
        return JsonNodeFactory.instance.objectNode().put("day", value.getDay().name())
                .put("time", value.getDisplayTime());
    }

    /**
     * Returns this owner's typed optional value without exposing the raw storage envelope.
     */
    public static Optional<WeeklySlot> get(Person person) {
        return person.getStudentFields().get(INSTANCE);
    }

    /**
     * Formats the complete slot, displaying an em dash when unset.
     */
    public static String display(Person person) {
        return get(person).map(WeeklySlot::toString).orElse("—");
    }
}
