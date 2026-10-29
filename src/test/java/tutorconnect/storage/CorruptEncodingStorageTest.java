package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.commons.util.FileUtil;

public class CorruptEncodingStorageTest {
    @TempDir
    private Path folder;

    @Test
    public void read_invalidUtf8InUnknownField_rejectsWithoutChangingBytes() throws Exception {
        String json = "{\"persons\":[{\"name\":\"Alex\",\"phone\":\"81234567\",\"address\":\"A\",\"future\":\"!\"}]}";
        for (byte invalid : new byte[]{(byte) 0x80, (byte) 0xc0, (byte) 0xff}) {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            bytes[json.indexOf('!')] = invalid;
            Path file = folder.resolve("corrupt.json");
            Files.write(file, bytes);
            assertThrows(DataLoadingException.class, () ->
                    new JsonStudentDirectoryStorage(file).readStudentDirectory());
            assertArrayEquals(bytes, Files.readAllBytes(file));
        }
    }

    @Test
    public void readAndWrite_validUnicode_preservesSupplementaryCharacters() throws Exception {
        Path file = folder.resolve("unicode.json");
        String json = "{\"persons\":[{\"name\":\"林明\",\"phone\":\"81234567\",\"address\":\"𠮷町\","
                + "\"future\":{\"text\":\"你好 😀\"}}]}";
        FileUtil.writeToFile(file, json);
        assertEquals(json, FileUtil.readFromFile(file));
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        var loaded = storage.readStudentDirectory().orElseThrow();
        storage.saveStudentDirectory(loaded);
        assertEquals(loaded, storage.readStudentDirectory().orElseThrow());
        assertEquals("你好 😀", loaded.getStudentList().getFirst().getStudentFields().toStorage()
                .get("future").get("text").asText());
    }

    @Test
    public void atomicWrite_unpairedSurrogate_preservesOldFileAndCleansTemporary() throws Exception {
        Path file = folder.resolve("students.json");
        String before = "{\"persons\":[]}";
        Files.writeString(file, before);
        assertThrows(IOException.class, () -> new AtomicJsonFile().write(file, "\uD800"));
        assertEquals(before, Files.readString(file));
        try (var files = Files.list(folder)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void atomicWrite_unpairedSurrogateWithoutOldFile_doesNotPublishReplacementCharacters() throws Exception {
        Path file = folder.resolve("students.json");
        assertThrows(IOException.class, () -> new AtomicJsonFile().write(file, "\uDC00"));
        assertFalse(Files.exists(file));
        try (var files = Files.list(folder)) {
            assertEquals(0, files.count());
        }
    }
}
