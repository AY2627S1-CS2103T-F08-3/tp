package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.Assert.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.commons.util.JsonUtil;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;

public class WeeklySlotPersistenceTest {
    @TempDir
    public Path directory;

    @Test
    public void saveAndReload_allWeekdays_preservesTypedSlotIdentityAndUnrelatedFields() throws Exception {
        Path file = directory.resolve("students.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        StudentFields unrelated = new StudentFields(Map.of("subject", JsonNodeFactory.instance.textNode("Math/Science"),
                "guardianPhone", JsonNodeFactory.instance.textNode("+6581234567"),
                "hourlyRate", JsonNodeFactory.instance.textNode("65.00")));
        for (DayOfWeek day : DayOfWeek.values()) {
            WeeklySlot slot = new WeeklySlot(day, LocalTime.of(9, 0));
            Student scheduled = ALICE.withStudentFields(unrelated.with(WeeklySlotField.INSTANCE, slot));
            StudentDirectory book = new StudentDirectory();
            book.addStudent(scheduled);
            storage.saveStudentDirectory(book);
            Student loaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
            assertEquals(scheduled, loaded);
            assertEquals(ALICE.getId(), loaded.getId());
            assertEquals(slot, WeeklySlotField.get(loaded).orElseThrow());
            assertEquals("Math/Science", loaded.getStudentFields().display("subject"));
            JsonNode json = JsonUtil.fromJsonString(Files.readString(file), JsonNode.class);
            assertEquals(day.name(), json.get("persons").get(0).get("weeklySlot").get("day").asText());
            assertEquals("09:00", json.get("persons").get(0).get("weeklySlot").get("time").asText());
        }
    }

    @Test
    public void read_legacyMissingOrNullSlot_remainsUnset() throws Exception {
        for (boolean explicitNull : List.of(false, true)) {
            ObjectNode record = JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedStudent(ALICE)),
                    ObjectNode.class);
            record.remove("id");
            record.remove("weeklySlot");
            if (explicitNull) {
                record.putNull("weeklySlot");
            }
            Path file = directory.resolve("legacy.json");
            Files.writeString(file, JsonNodeFactory.instance.objectNode()
                    .set("persons", JsonNodeFactory.instance.arrayNode().add(record)).toString());
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
            Student loaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
            assertTrue(WeeklySlotField.get(loaded).isEmpty());
            assertEquals("—", WeeklySlotField.display(loaded));
            assertEquals(loaded.getId(),
                    storage.readStudentDirectory().orElseThrow().getStudentList().getFirst().getId());
        }
    }

    @Test
    public void read_partialInvalidOrWronglyTypedSlots_rejectsEntireRecord() throws Exception {
        JsonNodeFactory json = JsonNodeFactory.instance;
        for (JsonNode slot : List.of(json.objectNode().put("day", "MONDAY"),
                json.objectNode().put("time", "09:00"), json.objectNode(), json.textNode("Monday 09:00"),
                json.objectNode().put("day", "Funday").put("time", "09:00"),
                json.objectNode().put("day", "MONDAY").put("time", "9:00"),
                json.objectNode().put("day", "MONDAY").put("time", "24:00"))) {
            ObjectNode record = JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedStudent(ALICE)),
                    ObjectNode.class);
            record.set("weeklySlot", slot);
            Path file = directory.resolve("invalid.json");
            String content = json.objectNode().set("persons", json.arrayNode().add(record)).toString();
            Files.writeString(file, content);
            assertThrows(DataLoadingException.class, ()
                -> new JsonStudentDirectoryStorage(file).readStudentDirectory());
            assertEquals(content, Files.readString(file));
        }
    }
}
