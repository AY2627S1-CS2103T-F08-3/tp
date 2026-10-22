package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;

/** Checks that saves and save failures leave an outcome record that names the command, not student details. */
public class LogicManagerLoggingTest {
    @TempDir
    private Path directory;

    @Test
    public void execute_savedChange_logsCommandAndCountWithoutStudentDetails() throws Exception {
        LogicManager logic = logic(new AtomicJsonFile());
        List<LogRecord> records = capture(() -> logic.execute("add n/Amy Tan p/81234567 a/21 Clementi Ave"));

        LogRecord saved = only(records, Level.INFO);
        assertEquals("AddCommand saved 1 student(s)", saved.getMessage());
        assertFalse(saved.getMessage().contains("81234567"));
    }

    @Test
    public void execute_saveFailure_logsWarningWithCause() throws Exception {
        LogicManager logic = logic(new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path target) throws IOException {
                throw new IOException("disk full");
            }
        });
        String input = "add n/Amy Tan p/81234567 a/21 Clementi Ave";
        List<LogRecord> records = capture(() -> assertThrows(CommandException.class, () -> logic.execute(input)));

        LogRecord failure = only(records, Level.WARNING);
        assertTrue(failure.getMessage().startsWith("AddCommand could not be saved"));
        assertInstanceOf(IOException.class, failure.getThrown());
        assertTrue(records.stream().noneMatch(record -> record.getLevel() == Level.INFO
                && record.getMessage().contains("saved")));
    }

    private LogicManager logic(AtomicJsonFile writer) {
        var storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"), writer);
        return new LogicManager(new ModelManager(), new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    /** Attaches after the logic is built because {@code LogsCenter.getLogger} resets the logger's handlers. */
    private List<LogRecord> capture(ThrowingRunnable action) throws Exception {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger logger = LogsCenter.getLogger(LogicManager.class);
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
        }
        return records;
    }

    /** Ignores the pre-existing raw {@code [USER COMMAND]} trace, which is outside this outcome check. */
    private LogRecord only(List<LogRecord> records, Level level) {
        List<LogRecord> matches = records.stream().filter(record -> record.getLevel() == level
                && !record.getMessage().contains("[USER COMMAND]")).toList();
        assertEquals(1, matches.size(), "Expected one " + level + " outcome record but got " + matches.size());
        return matches.getFirst();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
