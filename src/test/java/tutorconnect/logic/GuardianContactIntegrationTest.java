package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.ParserUtil;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.GuardianPhone;
import tutorconnect.model.student.HourlyRate;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.Subject;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** F04 acceptance gaps beyond the shared cross-feature integration tests. */
public class GuardianContactIntegrationTest {
    @TempDir
    private Path directory;
    private ModelManager model;
    private Logic logic;
    private JsonStudentDirectoryStorage storage;
    private RecordingWriter writer;
    private Path file;

    @BeforeEach
    public void setUp() throws Exception {
        StudentFields fields = new StudentFields(Map.of("futureField", TextNode.valueOf("keep me")))
                .with(EducationLevel.FIELD, new EducationLevel("Sec4"))
                .with(Subject.FIELD, new Subject("H2 Math"))
                .with(HourlyRate.FIELD, ParserUtil.parseHourlyRate("80"))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").withPhone("81234567")
                .build().withStudentFields(fields));
        model.addStudent(new StudentBuilder().withName("Nur Aisyah").withPhone("92345678").build());
        file = directory.resolve("students.json");
        writer = new RecordingWriter();
        storage = new JsonStudentDirectoryStorage(file, writer);
        storage.saveStudentDirectory(model.getStudentDirectory());
        writer.writes = 0;
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    @Test
    public void execute_sharedFamilyAndStudentPhones_preserveCompleteProfilesAfterRestart() throws Exception {
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        Student sibling = model.getStudentDirectory().getStudentList().getLast();
        assertEquals("Guardian contact set for Alex Tan: +6581234567.",
                logic.execute("guardian 1 g/+65 8123 4567").getFeedbackToUser());
        // A guardian may be the primary contact; siblings may share the same guardian.
        assertEquals("Guardian contact set for Nur Aisyah: +6581234567.",
                logic.execute("guardian 2 g/81234567").getFeedbackToUser());
        assertEquals("Guardian contact updated for Alex Tan: +6581234567 -> +6567778899.",
                logic.execute("guardian 1 g/+65-6777-8899").getFeedbackToUser());
        assertEquals(3, writer.writes);
        assertEquals(original.withGuardianPhone(new GuardianPhone("+6567778899")),
                model.getStudentDirectory().getStudentList().getFirst());
        assertEquals(sibling.withGuardianPhone(new GuardianPhone("+6581234567")),
                model.getStudentDirectory().getStudentList().getLast());
        ModelManager restarted =
                new ModelManager(new JsonStudentDirectoryStorage(file).readStudentDirectory().orElseThrow(),
                new UserPrefs());
        assertEquals(model.getStudentDirectory(), restarted.getStudentDirectory());
        assertEquals(List.of(original.getId(), sibling.getId()),
                restarted.getStudentDirectory().getStudentList().stream().map(Student::getId).toList());
        assertEquals(original.getId(), model.getSelectedStudentId());
        String json = Files.readString(file);
        assertEquals(2, json.split("\"guardianPhone\"", -1).length - 1);
    }

    @Test
    public void execute_firstAssignmentAndReplacement_writeFailuresDoNotPublish() throws Exception {
        for (boolean existing : List.of(false, true)) {
            if (existing) {
                logic.execute("guardian 1 g/91234567");
            }
            // Keep the target at displayed index 1 but preserve a different selected row.
            model.updateFilteredStudentList(student -> student.getName().fullName.contains("a"));
            model.selectStudent(model.getFilteredStudentList().getLast().getId());
            Student before = model.getFilteredStudentList().getFirst();
            var selected = model.getSelectedStudentId();
            var predicate = model.getStudentPredicate();
            String disk = Files.readString(file);
            writer.beforeWrite = () -> {
                assertSame(before, model.getFilteredStudentList().getFirst());
                assertEquals(selected, model.getSelectedStudentId());
            };
            for (boolean failMove : List.of(false, true)) {
                writer.failWrite = !failMove;
                writer.failMove = failMove;
                CommandException error = assertThrows(CommandException.class, ()
                    -> logic.execute("guardian 1 g/82345678"));
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, error.getMessage());
                assertSame(before, model.getFilteredStudentList().getFirst());
                assertSame(predicate, model.getStudentPredicate());
                assertEquals(selected, model.getSelectedStudentId());
                assertEquals(disk, Files.readString(file));
                try (var files = Files.list(directory)) {
                    assertEquals(List.of(file), files.toList());
                }
            }
            writer.failWrite = false;
            writer.failMove = false;
            writer.beforeWrite = () -> {};
        }
    }

    @Test
    public void execute_invalidValuesBeforeMissingRow_preserveStateAndAllowRecovery() throws Exception {
        Student target = model.getStudentDirectory().getStudentList().getLast();
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        model.selectStudent(target.getId());
        var predicate = model.getStudentPredicate();
        String disk = Files.readString(file);
        for (String command : List.of("guardian 999 g/bad", "guardian 1 g/", "guardian 1 g/81234567g/91234567")) {
            ParseException error = assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(ParserUtil.MESSAGE_INVALID_GUARDIAN_PHONE, error.getMessage());
        }
        for (String index : List.of("2", "9".repeat(100))) {
            CommandException error = assertThrows(CommandException.class, ()
                -> logic.execute("guardian " + index + " g/81234567"));
            assertEquals("Error: No student exists at index " + index + ".", error.getMessage());
        }
        assertEquals(0, writer.writes);
        assertSame(target, model.getFilteredStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(target.getId(), model.getSelectedStudentId());
        assertEquals(disk, Files.readString(file));
        assertEquals("Guardian contact set for Nur Aisyah: +6581234567.",
                logic.execute("guardian 1 g/81234567").getFeedbackToUser());
        Student updated = model.getFilteredStudentList().getFirst();
        disk = Files.readString(file);
        writer.failWrite = true;
        assertEquals("Guardian contact for Nur Aisyah is already +6581234567.",
                logic.execute("guardian 1 g/+65-8123-4567").getFeedbackToUser());
        assertEquals(1, writer.writes);
        assertSame(updated, model.getFilteredStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(disk, Files.readString(file));
    }

    private static class RecordingWriter extends AtomicJsonFile {
        private int writes;
        private boolean failWrite;
        private boolean failMove;
        private Runnable beforeWrite = () -> {};

        @Override
        public void write(Path destination, String json) throws IOException {
            writes++;
            beforeWrite.run();
            super.write(destination, json);
        }

        @Override
        protected void writeTemporary(Path temporary, String json) throws IOException {
            if (failWrite) {
                Files.writeString(temporary, "partial");
                throw new IOException("Injected partial write failure");
            }
            super.writeTemporary(temporary, json);
        }

        @Override
        protected void replace(Path temporary, Path destination) throws IOException {
            if (failMove) {
                throw new IOException("Injected atomic move failure");
            }
            super.replace(temporary, destination);
        }
    }
}
