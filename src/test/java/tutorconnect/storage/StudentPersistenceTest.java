package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.commons.exceptions.IllegalValueException;
import tutorconnect.commons.util.JsonUtil;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;

public class StudentPersistenceTest {
    private static final String OLD_RECORD = """
            {"name":"Alex Tan","phone":"81234567","address":"12/3 Street"}
            """;

    @TempDir
    private Path directory;

    @Test
    public void renamedDirectory_preservesLegacyJsonSchemaAndIdentity() throws Exception {
        String legacyJson = "{\"persons\":[" + OLD_RECORD + "]}";
        StudentDirectory loaded = JsonUtil.fromJsonString(legacyJson,
                JsonSerializableStudentDirectory.class).toModelType();
        String saved = JsonUtil.toJsonString(new JsonSerializableStudentDirectory(loaded));
        JsonNode tree = new ObjectMapper().readTree(saved);
        assertTrue(tree.has("persons"));
        assertFalse(tree.has("students"));
        assertEquals(1, tree.get("persons").size());
        StudentDirectory reloaded = JsonUtil.fromJsonString(saved,
                JsonSerializableStudentDirectory.class).toModelType();
        assertEquals(loaded, reloaded);
        assertEquals(loaded.getStudentList().getFirst().getId(), reloaded.getStudentList().getFirst().getId());
    }

    @Test
    public void oldRecord_missingIdAndOptionals_loadsStableIdentityAndUnsetFields() throws Exception {
        Student first = JsonUtil.fromJsonString(OLD_RECORD, JsonAdaptedStudent.class).toModelType();
        Student second = JsonUtil.fromJsonString(OLD_RECORD, JsonAdaptedStudent.class).toModelType();
        assertEquals(first.getId(), second.getId());
        assertEquals("+6581234567", first.getPhone().value);
        assertTrue(first.getStudentFields().toStorage().isEmpty());
        assertEquals("—", first.getStudentFields().display("guardianPhone"));
        assertTrue(first.getEmail().value.isEmpty());
        assertTrue(first.getTags().isEmpty());
    }

    @Test
    public void roundTrip_preservesOptionalOwnerFieldsAndLegacyData() throws Exception {
        String json = """
                {"name":"Alex Tan","phone":"+65 8123 4567","address":"12/3 Street",
                 "email":"alex@example.com","tags":["family"],"guardianPhone":"+6591234567",
                 "educationLevel":"SECONDARY_1","subject":"Mathematics","hourlyRate":"25.00",
                 "weeklySlot":{"day":"MONDAY","time":"15:30"},"futureOwnerField":{"a":1}}
                """;
        Student student = JsonUtil.fromJsonString(json, JsonAdaptedStudent.class).toModelType();
        StudentDirectory book = new StudentDirectory();
        book.addStudent(student);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        storage.saveStudentDirectory(book);
        Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
        assertEquals(student, reloaded);
        assertEquals(student.getId(), reloaded.getId());
        assertEquals(student.getStudentFields().toStorage(), reloaded.getStudentFields().toStorage());
    }

    @Test
    public void explicitNullOptional_isUnset() throws Exception {
        String json = OLD_RECORD.strip().replace("}", ",\"guardianPhone\":null}");
        Student student = JsonUtil.fromJsonString(json, JsonAdaptedStudent.class).toModelType();
        assertTrue(student.getStudentFields().toStorage().isEmpty());
    }

    @Test
    public void malformedOrDuplicateIds_rejected() throws Exception {
        String malformed = OLD_RECORD.strip().replace("}", ",\"id\":\"bad\"}");
        JsonAdaptedStudent adapter = JsonUtil.fromJsonString(malformed, JsonAdaptedStudent.class);
        assertThrows(IllegalValueException.class, adapter::toModelType);
        String id = "\"id\":\"12345678-1234-1234-1234-123456789abc\",";
        String first = OLD_RECORD.replace("{", "{" + id);
        String second = first.replace("Alex Tan", "Another Student");
        JsonSerializableStudentDirectory book = JsonUtil.fromJsonString("{\"persons\":[" + first + "," + second + "]}",
                JsonSerializableStudentDirectory.class);
        assertThrows(IllegalValueException.class, book::toModelType);
    }

    @Test
    public void optionalEnvelope_cannotOverwriteCoreFields() {
        for (String key : new String[]{"id", "name", "phone", "address", "email", "tags"}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new StudentFields(Map.of(key, new TextNode("replacement"))));
        }
    }

    @Test
    public void legacyGuardianRateJson_loadsTypedValuesIntoTheSharedEnvelope() throws Exception {
        JsonAdaptedStudent adapter = new JsonAdaptedStudent("Alex Tan", "81234567", null, "21 Street", null,
                "+6591234567", "40");
        Student loaded = adapter.toModelType();
        assertEquals("+6591234567", loaded.getGuardianPhone().orElseThrow().value);
        assertEquals("40.00", loaded.getHourlyRate().orElseThrow().toString());
        assertEquals("+6591234567", loaded.getStudentFields().display("guardianPhone"));
        assertEquals("40.00", loaded.getStudentFields().display("hourlyRate"));
        assertEquals(loaded.getId(), new JsonAdaptedStudent(loaded).toModelType().getId());
    }

    @Test
    public void invalidGuardianAndRateJson_reportValidationErrors() {
        for (String guardian : new String[]{"91234567", "invalid"}) {
            JsonAdaptedStudent adapter = new JsonAdaptedStudent("Alex", "81234567", null, "A", null, guardian, null);
            assertThrows(IllegalValueException.class, adapter::toModelType);
        }
        for (String rate : new String[]{"invalid", "0.00", "1000.01", "40.001"}) {
            JsonAdaptedStudent adapter = new JsonAdaptedStudent("Alex", "81234567", null, "A", null, null, rate);
            assertThrows(IllegalValueException.class, adapter::toModelType);
        }
    }
}
