package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class WeeklySlotFieldTest {
    @Test
    public void codec_canonicalCompletePair_roundTripsTypedValue() {
        WeeklySlot slot = new WeeklySlot(DayOfWeek.SATURDAY, LocalTime.of(9, 0));
        JsonNode encoded = WeeklySlotField.INSTANCE.encode(slot);
        assertEquals("SATURDAY", encoded.get("day").asText());
        assertEquals("09:00", encoded.get("time").asText());
        assertEquals(2, encoded.size());
        assertEquals(slot, WeeklySlotField.INSTANCE.decode(encoded));
        assertEquals("weeklySlot", WeeklySlotField.INSTANCE.key());
    }

    @Test
    public void codec_partialAndInvalidPairs_rejectsWithoutChangingEnvelope() {
        JsonNodeFactory json = JsonNodeFactory.instance;
        for (JsonNode value : List.of(json.objectNode(), json.objectNode().put("day", "MONDAY"),
                json.objectNode().put("time", "09:00"), json.textNode("Monday 09:00"),
                json.objectNode().put("day", "MONDAY").put("time", 9),
                json.objectNode().put("day", "Funday").put("time", "09:00"),
                json.objectNode().put("day", "MONDAY").put("time", "24:00"))) {
            assertThrows(IllegalArgumentException.class, () -> WeeklySlotField.INSTANCE.decode(value));
        }
        StudentFields fields = new StudentFields();
        assertThrows(NullPointerException.class, () -> fields.with(WeeklySlotField.INSTANCE, null));
        assertTrue(fields.get(WeeklySlotField.INSTANCE).isEmpty());
    }

    @Test
    public void typedUpdate_preservesUnrelatedDataAndIdentity_andUnsetDisplay() {
        assertEquals("—", WeeklySlotField.display(ALICE));
        StudentFields fields = new StudentFields(Map.of("subject", JsonNodeFactory.instance.textNode("Math/Science")));
        Person original = ALICE.withStudentFields(fields);
        WeeklySlot slot = new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(23, 59));
        Person updated = original.withStudentFields(fields.with(WeeklySlotField.INSTANCE, slot));
        assertEquals(original.getId(), updated.getId());
        assertEquals("Math/Science", updated.getStudentFields().display("subject"));
        assertEquals("Sunday 23:59", WeeklySlotField.display(updated));
        assertTrue(WeeklySlotField.get(original).isEmpty());
    }
}
