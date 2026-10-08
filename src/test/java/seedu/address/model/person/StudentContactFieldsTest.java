package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import seedu.address.testutil.PersonBuilder;

public class StudentContactFieldsTest {
    @Test
    public void legacyConstructorAndEnvelope_haveOneCanonicalRepresentation() {
        Person original = new PersonBuilder().build();
        GuardianPhone guardian = new GuardianPhone("+6591234567");
        HourlyRate rate = new HourlyRate(new BigDecimal("40"));
        Person fromLegacy = new Person(original.getName(), original.getPhone(), original.getEmail(),
                original.getAddress(), original.getTags(), Optional.of(guardian), Optional.of(rate));
        Person fromEnvelope = original.withStudentFields(new StudentFields(Map.of(
                "guardianPhone", TextNode.valueOf("+6591234567"), "hourlyRate", IntNode.valueOf(40))));
        assertEquals(fromLegacy, fromEnvelope);
        assertEquals(fromLegacy.hashCode(), fromEnvelope.hashCode());
        assertEquals(original.getId(), fromEnvelope.getId());
        assertNotEquals(original.getId(), fromLegacy.getId());
        assertEquals("40.00", fromEnvelope.getStudentFields().display("hourlyRate"));
        assertEquals(Optional.of(guardian), fromEnvelope.getGuardianPhone());
        assertEquals(Optional.of(rate), fromEnvelope.getHourlyRate());
        assertNotEquals(fromEnvelope, fromEnvelope.withHourlyRate(new HourlyRate(new BigDecimal("50"))));
        assertNotEquals(fromEnvelope, fromEnvelope.withGuardianPhone(new GuardianPhone("+6561234567")));
    }

    @Test
    public void invalidKnownFields_cannotEnterTheStudentEnvelope() {
        Person student = new PersonBuilder().build();
        for (var node : java.util.List.of(TextNode.valueOf("91234567"), IntNode.valueOf(91234567))) {
            assertThrows(IllegalArgumentException.class, () -> student.withStudentFields(
                    new StudentFields(Map.of("guardianPhone", node))));
        }
        for (var node : java.util.List.of(TextNode.valueOf("invalid"), TextNode.valueOf("0.00"),
            TextNode.valueOf("40.001"), JsonNodeFactory.instance.objectNode())) {
            assertThrows(IllegalArgumentException.class, () -> student.withStudentFields(
                    new StudentFields(Map.of("hourlyRate", node))));
        }
    }
}
