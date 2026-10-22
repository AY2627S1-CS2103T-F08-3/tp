package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.logic.commands.FilterCommand;
import tutorconnect.logic.commands.FindCommand;
import tutorconnect.logic.commands.ListCommand;
import tutorconnect.logic.parser.FilterCommandParser;
import tutorconnect.logic.parser.FindCommandParser;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.Subject;
import tutorconnect.testutil.StudentBuilder;

/** Checks useful discovery diagnostics without exposing names, field values or invalid input. */
class StudentDiscoveryLoggingTest {
    @Test
    void successfulQueries_logCountsAndCriteriaPresenceWithoutValues() throws Exception {
        ModelManager model = new ModelManager();
        Student student = new StudentBuilder().withName("Confidential Student").build();
        model.addStudent(student.withStudentFields(student.getStudentFields()
                .with(EducationLevel.FIELD, new EducationLevel("p1"))
                .with(Subject.FIELD, new Subject("Private Subject"))));
        FindCommandParser find = new FindCommandParser();
        FilterCommandParser filter = new FilterCommandParser();
        List<LogRecord> records = capture(() -> {
            find.parse("Confidential").execute(model);
            find.parse("MissingName").execute(model);
            filter.parse(" l/p1 s/Private Subject").execute(model);
            filter.parse(" s/MissingSubject").execute(model);
            new ListCommand().execute(model);
        });
        List<String> messages = records.stream().map(LogRecord::getMessage).toList();
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("Name query")
                && message.contains("matched 1 student(s)")));
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("Name query")
                && message.contains("matched 0 student(s)")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("level=true; subject=true; matched 1")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("level=false; subject=true; matched 0")));
        assertTrue(messages.contains("Full register view restored; 1 student(s); selection cleared"));
        assertEquals(1, model.getFilteredStudentList().size());
        assertSafe(records);
    }

    @Test
    void rejectedQueries_logCategoriesWithoutExceptionOrFalseSuccess() throws Exception {
        FindCommandParser find = new FindCommandParser();
        FilterCommandParser filter = new FilterCommandParser();
        List<LogRecord> records = capture(() -> {
            assertThrows(ParseException.class, () -> find.parse(" "));
            assertThrows(ParseException.class, () -> find.parse("Confidential\u200b"));
            assertThrows(ParseException.class, () -> filter.parse(" "));
            assertThrows(ParseException.class, () -> filter.parse(" s/Private Subject\n"));
            assertThrows(ParseException.class, () -> filter.parse(" secret/Confidential"));
            assertThrows(ParseException.class, () -> filter.parse(" s/Private Subject s/Math"));
            assertThrows(ParseException.class, () -> filter.parse(" l/InvalidLevel"));
        });
        List<String> messages = records.stream().map(LogRecord::getMessage).toList();
        assertEquals(7, messages.size());
        assertTrue(messages.contains("Name query rejected: missing keywords"));
        assertTrue(messages.contains("Name query rejected: prohibited text"));
        assertTrue(messages.contains("Criteria query rejected: missing criteria"));
        assertTrue(messages.contains("Criteria query rejected: prohibited text"));
        assertEquals(2, messages.stream()
                .filter(message -> message.equals("Criteria query rejected: invalid parameter structure")).count());
        assertTrue(messages.contains("Criteria query rejected: invalid criterion value"));
        assertFalse(messages.stream().anyMatch(message -> message.contains("matched")));
        assertSafe(records);
    }

    private void assertSafe(List<LogRecord> records) {
        for (LogRecord record : records) {
            assertEquals(Level.FINE, record.getLevel());
            assertNull(record.getThrown());
            assertNull(record.getParameters());
            for (String value : List.of("Confidential", "Private Subject", "MissingName", "MissingSubject",
                    "InvalidLevel", "secret/", "p1")) {
                assertFalse(record.getMessage().contains(value), record.getMessage());
            }
        }
    }

    private List<LogRecord> capture(CheckedAction action) throws Exception {
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
        handler.setLevel(Level.ALL);
        List<Class<?>> classes = List.of(FindCommand.class, FilterCommand.class, ListCommand.class,
                FindCommandParser.class, FilterCommandParser.class);
        List<Logger> loggers = new ArrayList<>();
        List<Level> previousLevels = new ArrayList<>();
        for (Class<?> type : classes) {
            Class.forName(type.getName()); // Initialize static loggers before attaching the recording handler.
            Logger logger = LogsCenter.getLogger(type);
            loggers.add(logger);
            previousLevels.add(logger.getLevel());
            logger.setLevel(Level.ALL);
            logger.addHandler(handler);
        }
        try {
            action.run();
        } finally {
            for (int i = 0; i < loggers.size(); i++) {
                loggers.get(i).removeHandler(handler);
                loggers.get(i).setLevel(previousLevels.get(i));
            }
        }
        return records;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
