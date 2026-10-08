package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import seedu.address.testutil.PersonBuilder;

public class StudentFieldsTest {
    @Test
    public void snapshots_preserveIdentityAndUnrelatedFields() {
        Person student = new Person(new Name("Alex Tan"), new Phone("81234567"), new Address("21 Clementi Ave"));
        assertTrue(student.getStudentFields().toStorage().isEmpty());
        assertEquals("—", student.getStudentFields().display("guardianPhone"));
        assertEquals("", student.getEmail().value);
        Person copy = student.withDetails(new Name("Alex Lee"), student.getPhone(), student.getEmail(),
                student.getAddress(), student.getTags());
        assertEquals(student.getId(), copy.getId());
        assertNotEquals(student.getId(), new PersonBuilder().build().getId());
    }

    @Test
    public void storageEnvelope_isDefensivelyCopied() {
        var node = JsonNodeFactory.instance.objectNode().put("day", "MONDAY");
        StudentFields fields = new StudentFields(Map.of("weeklySlot", node));
        node.put("day", "TUESDAY");
        var exported = fields.toStorage();
        ObjectNode exportedSlot = (ObjectNode) exported.get("weeklySlot");
        exportedSlot.removeAll();
        assertEquals("MONDAY", fields.toStorage().get("weeklySlot").get("day").asText());
    }
}
