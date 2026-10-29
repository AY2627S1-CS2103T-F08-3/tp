package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;

import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.commons.util.JsonUtil;

public class TrailingJsonContentTest {
    @TempDir
    private Path directory;

    @Test
    public void read_trailingDocumentOrToken_rejectsWithoutChangingFile() throws Exception {
        for (String suffix : new String[]{"{}", "[]", "null", "true", "123", "\"extra\"", "garbage"}) {
            String json = "{\"persons\":[]} " + suffix;
            Path file = directory.resolve("invalid.json");
            Files.writeString(file, json);
            assertThrows(DataLoadingException.class, () ->
                    new JsonStudentDirectoryStorage(file).readStudentDirectory());
            assertEquals(json, Files.readString(file));
        }
    }

    @Test
    public void read_trailingStudentDirectory_rejects() throws Exception {
        Path file = directory.resolve("two-directories.json");
        Files.writeString(file, "{\"persons\":[]} {\"persons\":["
                + "{\"name\":\"Alex\",\"phone\":\"81234567\",\"address\":\"A\"}]}");
        assertThrows(DataLoadingException.class, () -> new JsonStudentDirectoryStorage(file).readStudentDirectory());
    }

    @Test
    public void read_trailingWhitespace_loadsNormally() throws Exception {
        Path file = directory.resolve("valid.json");
        Files.writeString(file, " \n\t{\"persons\":[]}\r\n\t ");
        var loaded = new JsonStudentDirectoryStorage(file).readStudentDirectory().orElseThrow();
        assertTrue(loaded.getStudentList().isEmpty());
    }

    @Test
    public void fromJsonString_scalarOrArray_consumesCompleteValue() throws Exception {
        assertEquals(123, JsonUtil.fromJsonString("123 \n", Integer.class));
        assertEquals(2, JsonUtil.fromJsonString("[1,2] \t", JsonNode.class).size());
        assertThrows(IOException.class, () -> JsonUtil.fromJsonString("123 456", Integer.class));
        assertThrows(IOException.class, () -> JsonUtil.fromJsonString("[1,2] []", JsonNode.class));
        assertThrows(IOException.class, () -> JsonUtil.fromJsonString("null {}", JsonNode.class));
    }
}
