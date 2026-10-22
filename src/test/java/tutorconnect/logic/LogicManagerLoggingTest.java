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
import tutorconnect.logic.parser.TutorConnectParser;
import tutorconnect.logic.parser.exceptions.ParseException;
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
        assertTrue(records.stream().noneMatch(record -> record.getMessage().contains("staged model published")));
    }

    @Test
    public void execute_contactCommands_logsPublicationAfterSaveWithoutContactValues() throws Exception {
        LogicManager logic = logic(new AtomicJsonFile());
        List<LogRecord> records = capture(() -> {
            logic.execute("add n/Amy Tan p/81234567 a/21 Clementi Ave");
            logic.execute("edit 1 a/12 New Street");
            logic.execute("guardian 1 g/92345678");
            logic.execute("delete 1");
        });

        List<String> outcomes = records.stream().map(LogRecord::getMessage)
                .filter(message -> message.contains(" saved ") || message.contains("staged model published"))
                .toList();
        assertEquals(List.of("AddCommand saved 1 student(s)", "AddCommand completed; staged model published",
                "EditCommand saved 1 student(s)", "EditCommand completed; staged model published",
                "GuardianCommand saved 1 student(s)", "GuardianCommand completed; staged model published",
                "DeleteCommand saved 0 student(s)", "DeleteCommand completed; staged model published"), outcomes);
        assertNoContactValues(records);
    }

    @Test
    public void execute_rejectedInput_logsStageWithoutInputOrSuccess() throws Exception {
        LogicManager logic = logic(new AtomicJsonFile());
        logic.execute("add n/Amy Tan p/81234567 a/21 Clementi Ave");
        List<LogRecord> records = capture(() -> {
            assertThrows(ParseException.class, () -> logic.execute("Amy Tan 81234567"));
            assertThrows(ParseException.class, () -> logic.execute("edit 1 p/private-phone-value"));
            assertThrows(CommandException.class, () ->
                    logic.execute("add n/Amy Tan p/81234567 a/12 New Street"));
        });

        assertEquals(2, records.stream().filter(record -> record.getLevel() == Level.FINE
                && record.getMessage().equals("Command rejected during parsing; model unchanged")).count());
        assertTrue(records.stream().anyMatch(record -> record.getLevel() == Level.FINE
                && record.getMessage().equals("AddCommand rejected during execution; staged changes discarded")));
        assertTrue(records.stream().noneMatch(record -> record.getMessage().contains(" saved ")
                || record.getMessage().contains("staged model published")));
        assertTrue(records.stream().allMatch(record -> record.getThrown() == null));
        assertNoContactValues(records);
    }

    private void assertNoContactValues(List<LogRecord> records) {
        for (String value : List.of("Amy Tan", "81234567", "21 Clementi Ave", "12 New Street", "92345678",
                "private-phone-value")) {
            assertTrue(records.stream().noneMatch(record -> record.getMessage().contains(value)),
                    "Command traces must not contain contact value: " + value);
        }
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
        Logger parserLogger = LogsCenter.getLogger(TutorConnectParser.class);
        Level originalLevel = logger.getLevel();
        Level originalParserLevel = parserLogger.getLevel();
        logger.setLevel(Level.ALL);
        parserLogger.setLevel(Level.ALL);
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        parserLogger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
            parserLogger.removeHandler(handler);
            logger.setLevel(originalLevel);
            parserLogger.setLevel(originalParserLevel);
        }
        return records;
    }

    /** Finds the single outcome record at the requested level. */
    private LogRecord only(List<LogRecord> records, Level level) {
        List<LogRecord> matches = records.stream().filter(record -> record.getLevel() == level).toList();
        assertEquals(1, matches.size(), "Expected one " + level + " outcome record but got " + matches.size());
        return matches.getFirst();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
