package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.commons.util.JsonUtil;

public class StudentIdValidationTest {
    private static final String ID = "12345678-abcd-1234-abcd-123456789abc";
    private static final String RECORD = "\"name\":\"Alex\",\"phone\":\"81234567\",\"address\":\"A\"";

    @TempDir
    private Path directory;

    @Test
    public void read_noncanonicalIds_rejectsWithoutChangingFile() throws Exception {
        for (String id : new String[]{"1-1-1-1-1", "12345678-abcd-1-abcd-123456789abc",
            "012345678-abcd-1234-abcd-123456789abc", "", "not-a-uuid", " " + ID, ID + " "}) {
            assertRejected("{\"persons\":[{" + RECORD + ",\"id\":\"" + id + "\"}]}");
        }
    }

    @Test
    public void read_canonicalAndUppercaseIds_roundTripWithoutChangingIdentity() throws Exception {
        for (String id : new String[]{ID, "12345678-ABCD-1234-ABCD-123456789ABC"}) {
            Path file = directory.resolve("valid.json");
            Files.writeString(file, "{\"persons\":[{" + RECORD + ",\"id\":\"" + id + "\"}]}");
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
            var loaded = storage.readStudentDirectory().orElseThrow();
            assertEquals(UUID.fromString(ID), loaded.getStudentList().getFirst().getId());
            storage.saveStudentDirectory(loaded);
            assertEquals(UUID.fromString(ID), storage.readStudentDirectory().orElseThrow()
                    .getStudentList().getFirst().getId());
        }
    }

    @Test
    public void read_legacyIdMissingOrNull_derivesStableNormalizedIdentity() throws Exception {
        var missing = JsonUtil.fromJsonString("{" + RECORD + "}", JsonAdaptedStudent.class).toModelType();
        var explicitNull = JsonUtil.fromJsonString("{" + RECORD + ",\"id\":null}",
                JsonAdaptedStudent.class).toModelType();
        var normalized = JsonUtil.fromJsonString("{" + RECORD.replace("Alex", "alex")
                .replace("81234567", "+65 8123 4567") + "}", JsonAdaptedStudent.class).toModelType();
        assertEquals(missing.getId(), explicitNull.getId());
        assertEquals(missing.getId(), normalized.getId());
        assertEquals(missing.getId(), new JsonAdaptedStudent(missing).toModelType().getId());
    }

    @Test
    public void read_duplicateIdsWithDifferentCase_rejects() throws Exception {
        String first = "{" + RECORD + ",\"id\":\"" + ID + "\"}";
        String second = "{" + RECORD.replace("Alex", "Bob")
                + ",\"id\":\"12345678-ABCD-1234-ABCD-123456789ABC\"}";
        assertRejected("{\"persons\":[" + first + "," + second + "]}");
    }

    private void assertRejected(String json) throws Exception {
        Path file = directory.resolve("invalid.json");
        Files.writeString(file, json);
        assertThrows(DataLoadingException.class, () -> new JsonStudentDirectoryStorage(file).readStudentDirectory());
        assertEquals(json, Files.readString(file));
    }
}
