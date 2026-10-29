package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class UnreadableDataBackupTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-29T08:15:30Z"), ZoneOffset.UTC);

    @TempDir
    private Path directory;

    @Test
    public void copyAside_unreadableFile_keepsExactBytesBesideOriginal() throws IOException {
        Path data = directory.resolve("addressbook.json");
        String broken = "{\"persons\":[{\"name\":\"Amy\",\"phone\":\"12\"}]}";
        Files.writeString(data, broken);

        Path backup = UnreadableDataBackup.copyAside(data, CLOCK);

        assertEquals(directory.resolve("addressbook.json.unreadable-20261029-081530.bak"), backup);
        assertEquals(broken, Files.readString(backup));
        assertEquals(broken, Files.readString(data));
    }

    @Test
    public void copyAside_backupAlreadyExists_choosesNewNameWithoutOverwriting() throws IOException {
        Path data = directory.resolve("addressbook.json");
        Files.writeString(data, "first");
        Path first = UnreadableDataBackup.copyAside(data, CLOCK);
        Files.writeString(data, "second");

        Path second = UnreadableDataBackup.copyAside(data, CLOCK);

        assertEquals(directory.resolve("addressbook.json.unreadable-20261029-081530-2.bak"), second);
        assertEquals("first", Files.readString(first));
        assertEquals("second", Files.readString(second));
    }

    @Test
    public void copyAside_missingFile_throwsIoException() {
        Path missing = directory.resolve("missing.json");
        assertThrows(NoSuchFileException.class, () -> UnreadableDataBackup.copyAside(missing, CLOCK));
    }
}
