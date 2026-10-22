package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import tutorconnect.logic.parser.VisibleIndex;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

public class UnscheduleIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void remove_filteredTargetPreservesFieldsAndPersistsAcrossRestart() throws Exception {
        ModelManager model = model();
        Student untouched = model.getFilteredStudentList().getFirst();
        Student target = model.getFilteredStudentList().getLast();
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        var writes = new AtomicInteger();
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            public void write(Path path, String json) throws IOException {
                writes.incrementAndGet();
                super.write(path, json);
            }
        };
        var storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"), writer);
        var logic = logic(model, storage);
        assertEquals("Weekly lesson removed for Ben: Monday 09:00.",
                logic.execute("  unschedule   １  ").getFeedbackToUser());
        Student updated = model.getFilteredStudentList().getFirst();
        assertEquals(target.getId(), updated.getId());
        assertEquals(target.withStudentFields(target.getStudentFields().without(WeeklySlotField.INSTANCE)), updated);
        assertSame(untouched, model.getStudentDirectory().getStudentList().getFirst());
        assertEquals(target.getId(), model.getSelectedStudentId());
        assertEquals(List.of(untouched), logic.getWeeklySchedule());
        assertTrue(WeeklySlotField.get(updated).isEmpty());
        assertEquals(1, writes.get());

        model.selectStudent(null);
        var predicate = model.getStudentPredicate();
        assertEquals("No weekly lesson is scheduled for Ben.", logic.execute("unschedule 1").getFeedbackToUser());
        assertEquals(1, writes.get());
        assertEquals(null, model.getSelectedStudentId());
        assertSame(predicate, model.getStudentPredicate());
        assertSame(updated, model.getFilteredStudentList().getFirst());

        var reloaded = new JsonStudentDirectoryStorage(storage.getStudentDirectoryFilePath())
                .readStudentDirectory().orElseThrow();
        assertEquals(List.of(untouched, updated), reloaded.getStudentList());
        assertEquals(target.getId(), reloaded.getStudentList().getLast().getId());
        assertTrue(WeeklySlotField.get(reloaded.getStudentList().getLast()).isEmpty());
    }

    @Test
    public void failures_preserveBytesObjectsFilterSelectionAndSummary() throws Exception {
        for (boolean failAfterWrite : List.of(true, false)) {
            ModelManager model = model();
            Student target = model.getFilteredStudentList().getLast();
            model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
            model.selectStudent(target.getId());
            var predicate = model.getStudentPredicate();
            Path path = directory.resolve("students.json");
            new JsonStudentDirectoryStorage(path).saveStudentDirectory(model.getStudentDirectory());
            String before = Files.readString(path);
            AtomicJsonFile writer = new AtomicJsonFile() {
                @Override
                protected void writeTemporary(Path temporary, String json) throws IOException {
                    super.writeTemporary(temporary, json);
                    if (failAfterWrite) {
                        throw new IOException("Injected partial-write failure");
                    }
                }

                @Override
                protected void replace(Path temporary, Path destination) throws IOException {
                    throw new IOException("Injected atomic-move failure");
                }
            };
            var logic = logic(model, new JsonStudentDirectoryStorage(path, writer));
            var summary = List.copyOf(logic.getWeeklySchedule());
            assertEquals(LogicManager.MESSAGE_SAVE_FAILURE,
                    assertThrows(CommandException.class, () -> logic.execute("unschedule 1")).getMessage());
            assertEquals(before, Files.readString(path));
            assertEquals(summary, logic.getWeeklySchedule());
            assertSame(target, model.getFilteredStudentList().getFirst());
            assertEquals(target.getId(), model.getSelectedStudentId());
            assertSame(predicate, model.getStudentPredicate());
        }
    }

    @Test
    public void invalidInputAndOversizedIndices_leaveStateUnchanged() throws Exception {
        ModelManager model = model();
        var storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        storage.saveStudentDirectory(model.getStudentDirectory());
        String bytes = Files.readString(storage.getStudentDirectoryFilePath());
        var before = List.copyOf(model.getFilteredStudentList());
        var selection = model.getSelectedStudentId();
        var logic = logic(model, storage);
        for (String invalid : List.of("", "0", "01", "-1", "1.0", "one")) {
            assertEquals(VisibleIndex.MESSAGE_INVALID,
                    assertThrows(ParseException.class, () -> logic.execute("unschedule " + invalid)).getMessage());
        }
        for (String index : List.of("3", "9".repeat(100))) {
            assertEquals("Error: No student exists at index " + index + ".",
                    assertThrows(CommandException.class, () -> logic.execute("unschedule " + index)).getMessage());
        }
        for (String input : List.of("unschedule 1 extra", "unschedule 0 2", "\nunschedule 1", "unschedule 1\n")) {
            assertEquals("Error: Unexpected text after command.",
                    assertThrows(ParseException.class, () -> logic.execute(input)).getMessage());
        }
        assertEquals("Error: Unknown parameter: d/.",
                assertThrows(ParseException.class, () -> logic.execute("unschedule 0 d/Mon")).getMessage());
        assertEquals(before, model.getFilteredStudentList());
        assertEquals(selection, model.getSelectedStudentId());
        assertEquals(bytes, Files.readString(storage.getStudentDirectoryFilePath()));
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("guardianPhone", TextNode.valueOf("+6591234567"),
                "hourlyRate", TextNode.valueOf("40.00"), "educationLevel", TextNode.valueOf("Secondary 4"),
                "subject", TextNode.valueOf("Mathematics"), "futureField", TextNode.valueOf("keep")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(9, 0)));
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy").build().withStudentFields(fields));
        model.addStudent(new StudentBuilder().withName("Ben").build().withStudentFields(fields));
        return model;
    }

    private LogicManager logic(ModelManager model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
