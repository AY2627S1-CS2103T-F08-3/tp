package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
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

/** End-to-end tests for the {@code subject} command, using real parsing, commands and JSON persistence. */
public class SubjectIntegrationTest {
    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";

    @TempDir
    private Path directory;

    @Test
    public void setReplaceAndNoChange_useExactMessagesAndSaveOnlyRealChanges() throws Exception {
        ModelManager model = model();
        Student target = model.getStudentDirectory().getStudentList().getFirst();
        AtomicInteger writes = new AtomicInteger();
        Logic logic = logic(model, countingStorage(writes));

        assertEquals("Subject set for Alex Tan: O-Level Chemistry.",
                logic.execute("subject 1 s/O-Level Chemistry").getFeedbackToUser());
        assertEquals("Subject updated for Alex Tan: O-Level Chemistry -> H2 Math.",
                logic.execute("subject 1 s/H2 Math").getFeedbackToUser());
        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/H2 Math").getFeedbackToUser());
        assertEquals(2, writes.get());
        assertEquals(target.getId(), model.getSelectedStudentId());
        assertEquals("H2 Math", displayedSubject(model));
    }

    @Test
    public void caseOnlyOrSpacingOnlyDifference_isNoChangeAndKeepsTheStoredDisplayValue() throws Exception {
        ModelManager model = model();
        Logic setup = logic(model, new JsonStudentDirectoryStorage(directory.resolve("setup.json")));
        setup.execute("subject 1 s/H2 Math");
        Student first = model.getStudentDirectory().getStudentList().getFirst();
        Student selected = model.getStudentDirectory().getStudentList().getLast();
        model.updateFilteredStudentList(student -> true);
        model.selectStudent(selected.getId());
        var predicate = model.getStudentPredicate();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json")) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory state) {
                throw new AssertionError("No-change commands must not save");
            }
        };
        Logic logic = logic(model, storage);

        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/h2 math").getFeedbackToUser());
        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/  H2     MATH  ").getFeedbackToUser());
        assertSame(first, model.getStudentDirectory().getStudentList().getFirst());
        assertEquals("H2 Math", displayedSubject(model));
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(selected.getId(), model.getSelectedStudentId());
    }

    @Test
    public void aChangeInContent_replacesTheSubjectAndShowsTheNewDisplayCase() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));
        logic.execute("subject 1 s/H2 Math");

        assertEquals("Subject updated for Alex Tan: H2 Math -> h2 maths.",
                logic.execute("subject 1 s/h2   maths").getFeedbackToUser());
        assertEquals("h2 maths", displayedSubject(model));
    }

    @Test
    public void slashesAndAllowedSymbolsInsideTheSubject_areAccepted() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));
        for (String subject : List.of("Math/Science", "Physics 2/3", "Arts & Crafts", "Children's Literature",
                "English (Oral) / Writing", "Primary 6 English")) {
            logic.execute("subject 1 s/" + subject);
            assertEquals(subject, displayedSubject(model));
        }
    }

    @Test
    public void update_keepsIdPositionUnrelatedFieldsAndPersistsOnceAcrossReload() throws Exception {
        ModelManager model = model();
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        Student other = model.getStudentDirectory().getStudentList().getLast();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        Logic logic = logic(model, storage);

        logic.execute("subject 1 s/H2 Math");
        logic.execute("subject 1 s/O-Level Chemistry");

        Student updated = model.getStudentDirectory().getStudentList().getFirst();
        assertEquals(original.getId(), updated.getId());
        assertSame(other, model.getStudentDirectory().getStudentList().getLast());
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getPhone(), updated.getPhone());
        assertEquals(original.getAddress(), updated.getAddress());
        assertEquals(original.getGuardianPhone(), updated.getGuardianPhone());
        assertEquals(original.getHourlyRate(), updated.getHourlyRate());
        for (String key : List.of("educationLevel", "weeklySlot", "futureField")) {
            assertEquals(original.getStudentFields().toStorage().get(key),
                    updated.getStudentFields().toStorage().get(key), key);
        }
        assertEquals(new Subject("O-Level Chemistry"), updated.getStudentFields().get(Subject.FIELD).orElseThrow());

        String json = Files.readString(storage.getStudentDirectoryFilePath());
        assertEquals(1, json.split("\"subject\"", -1).length - 1, "replacement must not accumulate");
        assertTrue(json.contains("\"O-Level Chemistry\""));
        assertFalse(json.contains("H2 Math"));
        Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
        assertEquals(updated, reloaded);
        assertEquals("O-Level Chemistry", reloaded.getStudentFields().display("subject"));
    }

    @Test
    public void laterGuardianRateAndEditCommands_keepTheSubject() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));
        logic.execute("subject 1 s/H2 Math");
        logic.execute("guardian 1 g/92345678");
        logic.execute("rate 1 r/80");
        logic.execute("edit 1 a/22 New Street");
        assertEquals("H2 Math", displayedSubject(model));
    }

    @Test
    public void successfulUpdate_selectsTheTargetStudent() throws Exception {
        ModelManager model = model();
        Student last = model.getStudentDirectory().getStudentList().getLast();
        model.selectStudent(last.getId());
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));
        logic.execute("subject 1 s/Math");
        assertEquals(model.getStudentDirectory().getStudentList().getFirst().getId(), model.getSelectedStudentId());
    }

    @Test
    public void saveFailure_preservesFileModelFilterAndSelection() throws Exception {
        ModelManager model = model();
        Student target = model.getStudentDirectory().getStudentList().getFirst();
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        model.selectStudent(target.getId());
        var predicate = model.getStudentPredicate();
        List<Student> before = List.copyOf(model.getStudentDirectory().getStudentList());
        Path file = directory.resolve("students.json");
        new JsonStudentDirectoryStorage(file).saveStudentDirectory(model.getStudentDirectory());
        String saved = Files.readString(file);
        AtomicJsonFile failingWriter = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path destination) throws IOException {
                throw new IOException("Injected atomic replacement failure");
            }
        };
        Logic logic = logic(model, new JsonStudentDirectoryStorage(file, failingWriter));

        CommandException failure = assertThrows(CommandException.class, () -> logic.execute("subject 1 s/Math"));

        assertEquals("Error: Changes could not be saved. No changes were made.", failure.getMessage());
        assertEquals(saved, Files.readString(file));
        assertEquals(before, model.getStudentDirectory().getStudentList());
        assertSame(target, model.getFilteredStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(target.getId(), model.getSelectedStudentId());
    }

    @Test
    public void errors_useExactMessagesAndLeaveStateUntouched() throws Exception {
        ModelManager model = model();
        Student selected = model.getStudentDirectory().getStudentList().getLast();
        model.selectStudent(selected.getId());
        List<Student> before = List.copyOf(model.getStudentDirectory().getStudentList());
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));

        assertParseError(logic, "subject 1 s/Math@Home", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/A", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/12", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/" + "a".repeat(51), Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1", "Error: Missing required parameter: s/SUBJECT.");
        assertParseError(logic, "subject 1 s/Math s/Physics", "Error: Parameter s/ may be specified only once.");
        assertParseError(logic, "subject 1 l/Sec4", "Error: Unknown parameter: l/.");
        assertParseError(logic, "subject 1 foo s/Math", "Error: Unexpected text after command.");
        assertParseError(logic, "subject 0 s/Math", INVALID_INDEX);
        assertParseError(logic, "subject s/Math", INVALID_INDEX);
        assertParseError(logic, "subject abc s/@", INVALID_INDEX);
        // Values are validated before the row is looked up, so a bad value wins over a missing student.
        assertParseError(logic, "subject 99 s/@", Subject.MESSAGE_CONSTRAINTS);
        assertCommandError(logic, "subject 99 s/Math", "Error: No student exists at index 99.");
        String huge = "9".repeat(100);
        assertCommandError(logic, "subject " + huge + " s/Math", "Error: No student exists at index " + huge + ".");

        assertEquals(before, model.getStudentDirectory().getStudentList());
        assertEquals(selected.getId(), model.getSelectedStudentId());
    }

    @Test
    public void subject_resolvesTheDisplayedListAndManyStudentsMayShareASubject() throws Exception {
        ModelManager model = model();
        Student second = model.getStudentDirectory().getStudentList().getLast();
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));
        model.updateFilteredStudentList(student -> student.getId().equals(second.getId()));

        logic.execute("subject 1 s/H2 Math");
        model.updateFilteredStudentList(student -> true);
        logic.execute("subject 1 s/h2 math");

        for (Student student : model.getStudentDirectory().getStudentList()) {
            assertEquals(new Subject("H2 Math"), student.getStudentFields().get(Subject.FIELD).orElseThrow());
        }
    }

    @Test
    public void deletingAStudent_removesTheirSubjectFromStorage() throws Exception {
        ModelManager model = model();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        Logic logic = logic(model, storage);
        logic.execute("subject 1 s/H2 Math");
        assertTrue(Files.readString(storage.getStudentDirectoryFilePath()).contains("\"subject\""));

        logic.execute("delete 1");

        assertFalse(Files.readString(storage.getStudentDirectoryFilePath()).contains("\"subject\""));
    }

    @Test
    public void recordsWithoutASubject_loadAsUnsetAndShowAnEmDash() throws Exception {
        ModelManager model = model();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        storage.saveStudentDirectory(model.getStudentDirectory());

        Student loaded = storage.readStudentDirectory().orElseThrow().getStudentList().getLast();

        assertTrue(loaded.getStudentFields().get(Subject.FIELD).isEmpty());
        assertEquals("—", loaded.getStudentFields().display("subject"));
    }

    private static String displayedSubject(ModelManager model) {
        return model.getStudentDirectory().getStudentList().getFirst().getStudentFields().display("subject");
    }

    private static void assertParseError(Logic logic, String command, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> logic.execute(command)).getMessage(), command);
    }

    private static void assertCommandError(Logic logic, String command, String message) {
        assertEquals(message, assertThrows(CommandException.class, () -> logic.execute(command)).getMessage(),
                command);
    }

    private JsonStudentDirectoryStorage countingStorage(AtomicInteger writes) {
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            public void write(Path destination, String json) throws IOException {
                writes.incrementAndGet();
                super.write(destination, json);
            }
        };
        return new JsonStudentDirectoryStorage(directory.resolve("students.json"), writer);
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("guardianPhone", TextNode.valueOf("+6591234567"),
                "hourlyRate", TextNode.valueOf("40.00"), "educationLevel", TextNode.valueOf("Secondary 4"),
                "futureField", TextNode.valueOf("preserve me")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").build().withStudentFields(fields));
        model.addStudent(new StudentBuilder().withName("Other Student").withPhone("91234567").build());
        return model;
    }

    private Logic logic(ModelManager model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
