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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** Regression tests for the F01/F02/F03/F04 merge contracts, using real commands and JSON persistence. */
public class StudentFieldIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void guardianRateAndEdit_preserveIdPositionAndUnrelatedFieldsAcrossReload() throws Exception {
        ModelManager model = model();
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        Student other = model.getStudentDirectory().getStudentList().getLast();
        AtomicInteger writes = new AtomicInteger();
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            public void write(Path destination, String json) throws IOException {
                writes.incrementAndGet();
                super.write(destination, json);
            }
        };
        JsonStudentDirectoryStorage storage =
                new JsonStudentDirectoryStorage(directory.resolve("students.json"), writer);
        Logic logic = logic(model, storage);
        assertEquals("Guardian contact updated for Alex Tan: +6591234567 -> +6592345678.",
                logic.execute("guardian 1 g/+65 9234 5678").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$40.00 -> S$80.00.",
                logic.execute("rate 1 r/80").getFeedbackToUser());
        logic.execute("edit 1 a/22 New Street");
        assertEquals(3, writes.get()); // exactly one atomic replacement per mutation
        Student updated = model.getStudentDirectory().getStudentList().getFirst();
        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getId(), model.getSelectedStudentId());
        assertSame(other, model.getStudentDirectory().getStudentList().getLast());
        assertUnrelatedFields(original, updated);
        assertEquals("+6592345678", updated.getGuardianPhone().orElseThrow().value);
        assertEquals("80.00", updated.getHourlyRate().orElseThrow().toString());
        assertEquals("+6592345678", updated.getStudentFields().display("guardianPhone"));
        assertEquals("80.00", updated.getStudentFields().display("hourlyRate"));
        Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
        assertEquals(updated, reloaded);
        assertEquals(updated.getId(), reloaded.getId());
        String json = Files.readString(storage.getStudentDirectoryFilePath());
        assertEquals(1, json.split("\"guardianPhone\"", -1).length - 1);
        assertEquals(1, json.split("\"hourlyRate\"", -1).length - 1);
    }

    @Test
    public void equivalentUpdates_preserveSelectionFilterObjectsFormattingAndDoNotSave() throws Exception {
        ModelManager model = model();
        Student original = model.getStudentDirectory().getStudentList().getFirst();
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
        assertEquals("Guardian contact for Alex Tan is already +6591234567.",
                logic.execute("guardian 1 g/+65-9123-4567").getFeedbackToUser());
        assertEquals("Hourly rate for Alex Tan is already S$40.00.",
                logic.execute("rate 1 r/40.0").getFeedbackToUser());
        logic.execute("edit 1 n/Alex   Tan");
        assertSame(original, model.getStudentDirectory().getStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(selected.getId(), model.getSelectedStudentId());
        assertEquals("40.00", original.getStudentFields().display("hourlyRate"));
    }

    @Test
    public void failedFieldUpdatesAndDelete_preserveFileModelFilterAndSelection() throws Exception {
        for (String command : List.of("guardian 1 g/92345678", "rate 1 r/80", "edit 1 a/New Street", "delete 1")) {
            ModelManager model = model();
            Student target = model.getStudentDirectory().getStudentList().getFirst();
            model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
            model.selectStudent(target.getId());
            var predicate = model.getStudentPredicate();
            List<Student> before = List.copyOf(model.getStudentDirectory().getStudentList());
            Path file = directory.resolve("students.json");
            new JsonStudentDirectoryStorage(file).saveStudentDirectory(model.getStudentDirectory());
            String saved = Files.readString(file);
            AtomicJsonFile writer = new AtomicJsonFile() {
                @Override
                protected void replace(Path temporary, Path destination) throws IOException {
                    throw new IOException("Injected atomic replacement failure");
                }
            };
            Logic logic = logic(model, new JsonStudentDirectoryStorage(file, writer));
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());
            assertEquals(saved, Files.readString(file));
            assertEquals(before, model.getStudentDirectory().getStudentList());
            assertSame(target, model.getFilteredStudentList().getFirst());
            assertSame(predicate, model.getStudentPredicate());
            assertEquals(target.getId(), model.getSelectedStudentId());
        }
    }

    @Test
    public void fieldCommands_resolveFilteredIndicesAndHandleOversizedIndicesSafely() throws Exception {
        ModelManager model = model();
        Student target = model.getStudentDirectory().getStudentList().getLast();
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));
        logic.execute("guardian 1 g/61234567");
        assertEquals(target.getId(), model.getSelectedStudentId());
        assertEquals("+6561234567", model.getFilteredStudentList().getFirst().getGuardianPhone().orElseThrow().value);
        String huge = "9".repeat(100);
        for (String command : List.of("delete " + huge, "guardian " + huge + " g/81234567",
            "rate " + huge + " r/40", "edit " + huge + " n/Name")) {
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals("Error: No student exists at index " + huge + ".", failure.getMessage());
        }
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("guardianPhone", TextNode.valueOf("+6591234567"),
                "hourlyRate", TextNode.valueOf("40.00"), "educationLevel", TextNode.valueOf("SECONDARY_1"),
                "subject", TextNode.valueOf("Mathematics"), "futureField", TextNode.valueOf("preserve me")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").build().withStudentFields(fields));
        model.addStudent(new StudentBuilder().withName("Other Student").withPhone("91234567").build());
        return model;
    }

    private void assertUnrelatedFields(Student original, Student updated) {
        for (String key : List.of("educationLevel", "subject", "weeklySlot", "futureField")) {
            assertEquals(original.getStudentFields().toStorage().get(key),
                    updated.getStudentFields().toStorage().get(key));
        }
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getPhone(), updated.getPhone());
        assertEquals(original.getEmail(), updated.getEmail());
        assertEquals(original.getTags(), updated.getTags());
    }

    private Logic logic(ModelManager model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
