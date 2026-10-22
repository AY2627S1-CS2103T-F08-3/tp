package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.AcademicNote;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.AssessmentRecord;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.StudentBuilder;

/** The note and assessment commands log what they did and what they refused, without personal or tutoring text. */
public class AcademicRecordLoggingTest {

    private static final List<Class<?>> LOGGING_CLASSES =
            List.of(AssessmentUpdate.class, AssessCommand.class, UnassessCommand.class, ClearNoteCommand.class);

    /** Captures the loggers of the commands at FINE level for the duration of one test. */
    private static final class CapturedLogs implements AutoCloseable {
        private final List<LogRecord> records = new ArrayList<>();
        private final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        private final List<Logger> loggers = new ArrayList<>();
        private final List<Level> originalLevels = new ArrayList<>();

        private CapturedLogs() throws Exception {
            for (Class<?> loggingClass : LOGGING_CLASSES) {
                // Initialize the class first: LogsCenter removes handlers from a logger when it is created.
                Class.forName(loggingClass.getName());
                Logger logger = LogsCenter.getLogger(loggingClass.getSimpleName());
                loggers.add(logger);
                originalLevels.add(logger.getLevel());
            }
            for (Logger logger : loggers) {
                logger.addHandler(handler);
                logger.setLevel(Level.FINE);
            }
        }

        private List<String> messages(Level level) {
            return records.stream().filter(record -> record.getLevel().equals(level))
                    .map(LogRecord::getMessage).toList();
        }

        private List<String> allMessages() {
            return records.stream().map(LogRecord::getMessage).toList();
        }

        @Override
        public void close() {
            for (int i = 0; i < loggers.size(); i++) {
                loggers.get(i).removeHandler(handler);
                loggers.get(i).setLevel(originalLevels.get(i));
            }
        }
    }

    private static ModelManager modelWith(Student student) {
        ModelManager model = new ModelManager();
        model.addStudent(student);
        return model;
    }

    private static Student studentWithStored(String key, String value) {
        return new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields(Map.of(key, TextNode.valueOf(value))));
    }

    @Test
    public void unreadableHistory_logsOneWarningNamingTheStudentIdButNotTheName() throws Exception {
        Student student = studentWithStored("assessments", "junk");
        ModelManager model = modelWith(student);
        AssessmentRecord record = AssessmentRecord.of("2026-10-01", "Secret Title", "Z9");
        List<Command> commands = List.of(new AssessCommand(INDEX_FIRST_STUDENT, record),
                new AssessmentsCommand(INDEX_FIRST_STUDENT), new UnassessCommand(INDEX_FIRST_STUDENT,
                        INDEX_FIRST_STUDENT));

        for (Command command : commands) {
            try (CapturedLogs logs = new CapturedLogs()) {
                assertThrows(CommandException.class, () -> command.execute(model));

                List<String> warnings = logs.messages(Level.WARNING);
                assertEquals(1, warnings.size(), command.toString());
                assertTrue(warnings.get(0).contains(student.getId().toString()), warnings.get(0));
                assertTrue(warnings.get(0).contains("could not be read"), warnings.get(0));
                assertFalse(warnings.get(0).contains("Amy"), warnings.get(0));
                assertFalse(warnings.get(0).contains("Bee"), warnings.get(0));
                assertFalse(warnings.get(0).contains("junk"), warnings.get(0));
            }
        }
    }

    @Test
    public void addingARecord_logsTheNumberAndTheStudentIdOnly() throws Exception {
        Student student = new StudentBuilder().withName("Amy Bee").build();
        ModelManager model = modelWith(student);

        try (CapturedLogs logs = new CapturedLogs()) {
            new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-10-01", "Secret Title", "Z9"))
                    .execute(model);

            assertEquals(List.of("Added assessment #1 for student " + student.getId()), logs.messages(Level.FINE));
            assertTrue(logs.messages(Level.WARNING).isEmpty());
            for (String message : logs.allMessages()) {
                assertFalse(message.contains("Secret") || message.contains("Z9") || message.contains("Amy"), message);
            }
        }
    }

    @Test
    public void aDuplicateOrAFullHistory_isLoggedWhenRejected() throws Exception {
        Student student = new StudentBuilder().withName("Amy Bee").build();
        ModelManager model = modelWith(student);
        new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-10-01", "Quiz", "A")).execute(model);
        AssessCommand duplicate = new AssessCommand(INDEX_FIRST_STUDENT,
                AssessmentRecord.of("2026-10-01", "  QUIZ ", "B"));
        for (int i = 2; i <= AssessmentHistory.MAX_RECORDS; i++) {
            new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-01-01", "Test " + i, "A")).execute(model);
        }
        AssessCommand overflow = new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-02-01", "More", "A"));

        try (CapturedLogs logs = new CapturedLogs()) {
            assertThrows(CommandException.class, () -> duplicate.execute(model));
            assertThrows(CommandException.class, () -> overflow.execute(model));

            assertEquals(List.of("Rejected a duplicate assessment dated 2026-10-01 for student " + student.getId(),
                    "Rejected an assessment for student " + student.getId() + ": the history is full"),
                    logs.messages(Level.FINE));
        }
    }

    @Test
    public void removingARecord_logsItAndAMissingRecordNumberIsLogged() throws Exception {
        Student student = new StudentBuilder().withName("Amy Bee").build();
        ModelManager model = modelWith(student);
        new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-10-01", "Quiz", "A")).execute(model);
        UnassessCommand missing = new UnassessCommand(INDEX_FIRST_STUDENT, Index.fromOneBased(5));

        try (CapturedLogs logs = new CapturedLogs()) {
            assertThrows(CommandException.class, () -> missing.execute(model));
            new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_FIRST_STUDENT).execute(model);

            assertEquals(List.of("Student " + student.getId() + " has no assessment record 5",
                    "Removed assessment #1 from student " + student.getId()), logs.messages(Level.FINE));
            assertTrue(logs.messages(Level.WARNING).isEmpty());
        }
    }

    @Test
    public void clearingANote_logsItWithoutTheNoteText() throws Exception {
        Student student = new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields().with(AcademicNote.FIELD,
                        new AcademicNote("private note text")));
        ModelManager model = modelWith(student);

        try (CapturedLogs logs = new CapturedLogs()) {
            new ClearNoteCommand(INDEX_FIRST_STUDENT).execute(model);

            assertEquals(List.of("Cleared the note of student " + student.getId()), logs.messages(Level.FINE));
            assertTrue(logs.messages(Level.WARNING).isEmpty());
            for (String message : logs.allMessages()) {
                assertFalse(message.contains("private") || message.contains("Amy"), message);
            }
        }
    }

    @Test
    public void clearingAnUnreadableNote_logsAWarning() throws Exception {
        Student student = new StudentBuilder().withName("Amy Bee").build().withStudentFields(
                new StudentFields(Map.of("academicNote", IntNode.valueOf(3))));
        ModelManager model = modelWith(student);

        try (CapturedLogs logs = new CapturedLogs()) {
            new ClearNoteCommand(INDEX_FIRST_STUDENT).execute(model);

            assertEquals(List.of("Clearing a stored note of student " + student.getId() + " that could not be read"),
                    logs.messages(Level.WARNING));
            assertEquals(List.of("Cleared the note of student " + student.getId()), logs.messages(Level.FINE));
        }
    }

    @Test
    public void clearingANoteThatIsNotThere_logsNothing() throws Exception {
        ModelManager model = modelWith(new StudentBuilder().withName("Amy Bee").build());

        try (CapturedLogs logs = new CapturedLogs()) {
            new ClearNoteCommand(INDEX_FIRST_STUDENT).execute(model);

            assertTrue(logs.allMessages().isEmpty());
        }
    }
}
