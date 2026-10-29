package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.commons.exceptions.DataLoadingException;

public class DuplicateJsonKeysTest {
    private static final String RECORD = "\"name\":\"Alex\",\"phone\":\"81234567\",\"address\":\"A\"";

    @TempDir
    private Path directory;

    @Test
    public void read_duplicateRootKeys_rejectsWithoutChangingFile() throws Exception {
        assertRejected("{\"persons\":[{" + RECORD + "}],\"persons\":[]}");
        assertRejected("{\"persons\":[],\"persons\":[{" + RECORD + "}]}");
    }

    @Test
    public void read_duplicateStudentKeys_rejectsWithoutChangingFile() throws Exception {
        assertRejected("{\"persons\":[{" + RECORD + ",\"name\":\"Bob\"}]}");
        assertRejected("{\"persons\":[{" + RECORD + ",\"phone\":\"91234567\"}]}");
    }

    @Test
    public void read_duplicateUnknownNestedKeys_rejectsWithoutChangingFile() throws Exception {
        assertRejected("{\"persons\":[{" + RECORD + ",\"future\":{\"value\":1,\"value\":2}}]}");
    }

    @Test
    public void read_repeatedKeysInSeparateObjects_preservesUnknownFieldsOnRoundTrip() throws Exception {
        Path file = directory.resolve("valid.json");
        Files.writeString(file, "{\"persons\":[{" + RECORD
                + ",\"future\":{\"left\":{\"value\":1},\"right\":{\"value\":2}}}]}");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        var loaded = storage.readStudentDirectory().orElseThrow();
        storage.saveStudentDirectory(loaded);
        assertEquals(loaded, storage.readStudentDirectory().orElseThrow());
        assertEquals(2, loaded.getStudentList().getFirst().getStudentFields().toStorage()
                .get("future").get("right").get("value").asInt());
    }

    private void assertRejected(String json) throws Exception {
        Path file = directory.resolve("invalid.json");
        Files.writeString(file, json);
        assertThrows(DataLoadingException.class, () -> new JsonStudentDirectoryStorage(file).readStudentDirectory());
        assertEquals(json, Files.readString(file));
    }
}
