package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.TypicalStudents.ALICE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.commons.util.JsonUtil;
import tutorconnect.model.student.Student;

/** Guardian JSON compatibility, including correctly and incorrectly edited optional values. */
public class GuardianContactStorageTest {
    @TempDir
    private Path directory;

    @Test
    public void read_missingOrNullGuardian_roundTripsAsUnsetWithStableIdentity() throws Exception {
        for (boolean explicitNull : List.of(false, true)) {
            ObjectNode record = record();
            record.remove("id");
            record.remove("guardianPhone");
            if (explicitNull) {
                record.putNull("guardianPhone");
            }
            Path file = write(record);
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
            var loaded = storage.readStudentDirectory().orElseThrow();
            Student student = loaded.getStudentList().getFirst();
            assertTrue(student.getGuardianPhone().isEmpty());
            assertEquals("—", student.getStudentFields().display("guardianPhone"));
            storage.saveStudentDirectory(loaded);
            Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
            assertEquals(student, reloaded);
            assertEquals(student.getId(), reloaded.getId());
        }
    }

    @Test
    public void read_manuallyEditedCanonicalGuardian_isAcceptedAndResaved() throws Exception {
        Path file = write(record().put("guardianPhone", "+6567778899"));
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        var loaded = storage.readStudentDirectory().orElseThrow();
        assertEquals("+6567778899", loaded.getStudentList().getFirst().getGuardianPhone().orElseThrow().value);
        storage.saveStudentDirectory(loaded);
        assertEquals(loaded, new JsonStudentDirectoryStorage(file).readStudentDirectory().orElseThrow());
    }

    @Test
    public void read_invalidGuardian_rejectsFileWithoutChangingBytes() throws Exception {
        for (String invalid : List.of("", "91234567", "+65 9123 4567", "+6571234567", "+65912345678")) {
            Path file = write(record().put("guardianPhone", invalid));
            String before = Files.readString(file);
            assertThrows(DataLoadingException.class, ()
                -> new JsonStudentDirectoryStorage(file).readStudentDirectory());
            assertEquals(before, Files.readString(file));
        }
    }

    private ObjectNode record() throws Exception {
        return JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedStudent(ALICE)), ObjectNode.class);
    }

    private Path write(ObjectNode record) throws Exception {
        Path file = directory.resolve("students.json");
        JsonNodeFactory json = JsonNodeFactory.instance;
        Files.writeString(file, json.objectNode().set("persons", json.arrayNode().add(record)).toString());
        return file;
    }
}
