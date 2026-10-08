package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AtomicJsonFileTest {
    @TempDir
    public Path directory;

    @Test
    public void write_completeReplacement() throws Exception {
        Path file = directory.resolve("students.json");
        Files.writeString(file, "old");
        new AtomicJsonFile().write(file, "new");
        assertEquals("new", Files.readString(file));
    }

    @Test
    public void write_failedReplacement_preservesOldFileAndCleansTemporary() throws Exception {
        Path file = directory.resolve("students.json");
        Files.writeString(file, "old");
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path target) throws IOException {
                throw new IOException("simulated move failure");
            }
        };
        assertThrows(IOException.class, () -> writer.write(file, "new"));
        assertEquals("old", Files.readString(file));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void write_partialTemporaryFailure_leavesNoTarget() throws Exception {
        Path file = directory.resolve("students.json");
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            protected void writeTemporary(Path temporary, String json) throws IOException {
                Files.writeString(temporary, "partial");
                throw new IOException("simulated disk full");
            }
        };
        assertThrows(IOException.class, () -> writer.write(file, "new"));
        assertFalse(Files.exists(file));
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }
}
