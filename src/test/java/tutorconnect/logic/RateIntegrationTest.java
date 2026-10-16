package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
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
import tutorconnect.logic.parser.ParserUtil;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.student.HourlyRate;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** F07 acceptance through the real parser, displayed model and atomic JSON storage. */
public class RateIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void setReplaceAndEquivalentAmounts_useExactFeedbackAndSaveOnlyChanges() throws Exception {
        ModelManager model = model();
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        Student other = model.getStudentDirectory().getStudentList().getLast();
        AtomicInteger writes = new AtomicInteger();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json")) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) throws IOException {
                writes.incrementAndGet();
                super.saveStudentDirectory(proposed);
            }
        };
        Logic logic = logic(model, storage);
        assertEquals("Hourly rate set for Alex Tan: S$1.00.",
                logic.execute("rate 1 r/1").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$1.00 -> S$1000.00.",
                logic.execute("rate 1 r/1000").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$1000.00 -> S$19.99.",
                logic.execute("rate 1 r/19.99").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$19.99 -> S$40.00.",
                logic.execute("rate 1 r/40").getFeedbackToUser());
        Student updated = model.getStudentDirectory().getStudentList().getFirst();
        assertEquals(original.getId(), model.getSelectedStudentId());
        assertSame(other, model.getStudentDirectory().getStudentList().getLast());
        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getPhone(), updated.getPhone());
        assertEquals(original.getAddress(), updated.getAddress());
        assertEquals(original.getEmail(), updated.getEmail());
        assertEquals(original.getTags(), updated.getTags());
        var unrelated = updated.getStudentFields().toStorage();
        unrelated.remove("hourlyRate");
        assertEquals(original.getStudentFields().toStorage(), unrelated);
        assertEquals(updated, storage.readStudentDirectory().orElseThrow().getStudentList().getFirst());

        model.selectStudent(other.getId());
        var predicate = model.getStudentPredicate();
        byte[] saved = Files.readAllBytes(storage.getStudentDirectoryFilePath());
        for (String amount : List.of("40", "40.0", "40.00")) {
            assertEquals("Hourly rate for Alex Tan is already S$40.00.",
                    logic.execute("rate 1 r/" + amount).getFeedbackToUser());
            assertSame(updated, model.getStudentDirectory().getStudentList().getFirst());
            assertSame(predicate, model.getStudentPredicate());
            assertEquals(other.getId(), model.getSelectedStudentId());
            assertArrayEquals(saved, Files.readAllBytes(storage.getStudentDirectoryFilePath()));
        }
        assertEquals(4, writes.get());
    }

    @Test
    public void filteredIndex_updatesDisplayedStudentAndRejectsHiddenStoragePosition() throws Exception {
        ModelManager model = model();
        Student hidden = model.getStudentDirectory().getStudentList().getFirst();
        Student displayed = model.getStudentDirectory().getStudentList().getLast();
        Logic logic = logic(model, new JsonStudentDirectoryStorage(directory.resolve("students.json")));
        logic.execute("find Other");
        var predicate = model.getStudentPredicate();
        assertEquals(List.of(displayed), model.getFilteredStudentList());
        logic.execute("rate 1 r/80.25");
        assertSame(hidden, model.getStudentDirectory().getStudentList().getFirst());
        assertEquals(displayed.getId(), model.getFilteredStudentList().getFirst().getId());
        assertEquals(new HourlyRate(new BigDecimal("80.25")),
                model.getFilteredStudentList().getFirst().getHourlyRate().orElseThrow());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(displayed.getId(), model.getSelectedStudentId());
        for (String index : List.of("2", "9".repeat(100))) {
            CommandException failure = assertThrows(CommandException.class, () ->
                    logic.execute("rate " + index + " r/40"));
            assertEquals("Error: No student exists at index " + index + ".", failure.getMessage());
        }
        logic.execute("find Nobody");
        assertThrows(CommandException.class, () -> logic.execute("rate 1 r/40"));
        ParseException invalidRate = assertThrows(ParseException.class, () -> logic.execute("rate 99 r/bad"));
        assertEquals(ParserUtil.MESSAGE_INVALID_HOURLY_RATE, invalidRate.getMessage());
    }

    @Test
    public void invalidInput_preservesFileObjectsFilterAndSelection() throws Exception {
        ModelManager model = model();
        Student target = model.getStudentDirectory().getStudentList().getLast();
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        model.selectStudent(target.getId());
        var predicate = model.getStudentPredicate();
        List<Student> before = List.copyOf(model.getStudentDirectory().getStudentList());
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        storage.saveStudentDirectory(model.getStudentDirectory());
        byte[] saved = Files.readAllBytes(storage.getStudentDirectoryFilePath());
        Logic logic = logic(model, storage);
        for (String command : List.of("rate 1 r/0", "rate 1 r/40.000", "rate 1 r/",
                "rate 1", "rate 1 r/40 r/50", "rate 01 r/40", "rate 1 x/40")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(before, model.getStudentDirectory().getStudentList());
            assertSame(target, model.getFilteredStudentList().getFirst());
            assertSame(predicate, model.getStudentPredicate());
            assertEquals(target.getId(), model.getSelectedStudentId());
            assertArrayEquals(saved, Files.readAllBytes(storage.getStudentDirectoryFilePath()));
        }
    }

    @Test
    public void failedFirstSetAndReplacement_rollBackAtWriteAndAtomicMove() throws Exception {
        for (boolean existingRate : List.of(false, true)) {
            for (boolean failReplace : List.of(false, true)) {
                ModelManager model = model();
                if (existingRate) {
                    Student target = model.getStudentDirectory().getStudentList().getFirst();
                    model.setStudent(target, target.withHourlyRate(new HourlyRate(new BigDecimal("40"))));
                }
                Student target = model.getStudentDirectory().getStudentList().getFirst();
                Student selected = model.getStudentDirectory().getStudentList().getLast();
                model.updateFilteredStudentList(student -> true);
                model.selectStudent(selected.getId());
                var predicate = model.getStudentPredicate();
                List<Student> before = List.copyOf(model.getStudentDirectory().getStudentList());
                Path file = directory.resolve("students.json");
                new JsonStudentDirectoryStorage(file).saveStudentDirectory(model.getStudentDirectory());
                byte[] saved = Files.readAllBytes(file);
                AtomicJsonFile writer = new AtomicJsonFile() {
                    @Override
                    protected void writeTemporary(Path temporary, String json) throws IOException {
                        super.writeTemporary(temporary, json);
                        if (!failReplace) {
                            throw new IOException("Injected failure after temporary write");
                        }
                    }

                    @Override
                    protected void replace(Path temporary, Path destination) throws IOException {
                        throw new IOException("Injected atomic replacement failure");
                    }
                };
                Logic logic = logic(model, new JsonStudentDirectoryStorage(file, writer));
                CommandException failure = assertThrows(CommandException.class, () -> logic.execute("rate 1 r/80"));
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());
                assertArrayEquals(saved, Files.readAllBytes(file));
                assertEquals(before, model.getStudentDirectory().getStudentList());
                assertSame(target, model.getStudentDirectory().getStudentList().getFirst());
                assertSame(selected, model.getStudentDirectory().getStudentList().getLast());
                assertSame(predicate, model.getStudentPredicate());
                assertEquals(selected.getId(), model.getSelectedStudentId());
                try (var files = Files.list(directory)) {
                    assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".student-save-")));
                }
            }
        }
    }

    @Test
    public void failedInitialSave_doesNotCreateRegisterOrPublishRate() {
        ModelManager model = model();
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        Path file = directory.resolve("new.json");
        Logic logic = logic(model, new JsonStudentDirectoryStorage(file, new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path destination) throws IOException {
                throw new IOException("Injected initial save failure");
            }
        }));
        assertThrows(CommandException.class, () -> logic.execute("rate 1 r/40"));
        assertFalse(Files.exists(file));
        assertSame(original, model.getStudentDirectory().getStudentList().getFirst());
        assertTrue(original.getHourlyRate().isEmpty());
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("guardianPhone", TextNode.valueOf("+6591234567"),
                "educationLevel", TextNode.valueOf("Secondary 1"), "subject", TextNode.valueOf("Mathematics"),
                "futureField", TextNode.valueOf("preserve me")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").withTags("family")
                .build().withStudentFields(fields));
        model.addStudent(new StudentBuilder().withName("Other Student").withPhone("91234567").build());
        return model;
    }

    private Logic logic(ModelManager model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
