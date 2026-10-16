package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.student.Address;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;

public class AtomicStudentCommandTest {
    @TempDir
    private Path directory;

    @Test
    public void add_savesCompleteStateBeforePublishingAndSurvivesReload() throws Exception {
        ModelManager model = existingModel();
        Student first = model.getStudentDirectory().getStudentList().getFirst();
        AtomicInteger saves = new AtomicInteger();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json")) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) throws IOException {
                assertEquals(List.of(first), model.getStudentDirectory().getStudentList());
                assertEquals(first.getId(), model.getSelectedStudentId());
                assertEquals(2, proposed.getStudentList().size());
                assertEquals("21 Clementi Ave 3 #04-18", proposed.getStudentList().getLast().getAddress().value);
                super.saveStudentDirectory(proposed);
                saves.incrementAndGet();
            }
        };
        Logic logic = logic(model, storage);
        assertEquals("Student added: Alex Tan.", logic.execute(
                "  add   n/Ａlex　 Tan p/+65-8123-4567 a/21 Clementi Ave 3 #04-18  ").getFeedbackToUser());
        Student added = model.getStudentDirectory().getStudentList().getLast();
        assertEquals(1, saves.get());
        assertEquals("+6581234567", added.getPhone().value);
        assertTrue(added.getStudentFields().toStorage().isEmpty());
        assertTrue(added.getEmail().value.isEmpty());
        assertTrue(added.getTags().isEmpty());
        assertEquals(added.getId(), model.getSelectedStudentId());
        assertEquals(2, model.getFilteredStudentList().size());
        ReadOnlyStudentDirectory reloaded = storage.readStudentDirectory().orElseThrow();
        assertEquals(model.getStudentDirectory(), reloaded);
        assertEquals(first.getId(), reloaded.getStudentList().getFirst().getId());
        assertEquals(added.getId(), reloaded.getStudentList().getLast().getId());
    }

    @Test
    public void add_failedReplacementPreservesFileModelFilterAndSelection() throws Exception {
        ModelManager model = existingModel();
        Student first = model.getStudentDirectory().getStudentList().getFirst();
        Path file = directory.resolve("students.json");
        new JsonStudentDirectoryStorage(file).saveStudentDirectory(model.getStudentDirectory());
        byte[] before = Files.readAllBytes(file);
        AtomicJsonFile failingWriter = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path target) throws IOException {
                throw new IOException("Injected atomic replacement failure");
            }
        };
        Logic logic = logic(model, new JsonStudentDirectoryStorage(file, failingWriter));
        var predicate = model.getStudentPredicate();
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("add n/Alex Tan p/81234567 a/21 Clementi Ave"));
        assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, error.getMessage());
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(List.of(first), model.getStudentDirectory().getStudentList());
        assertEquals(List.of(first), model.getFilteredStudentList());
        assertEquals(predicate, model.getStudentPredicate());
        assertEquals(first.getId(), model.getSelectedStudentId());
        try (var files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    public void add_firstSaveFails_doesNotCreateFileOrProfile() {
        ModelManager model = new ModelManager();
        Path file = directory.resolve("students.json");
        AtomicJsonFile failingWriter = new AtomicJsonFile() {
            @Override
            protected void writeTemporary(Path temporary, String json) throws IOException {
                throw new IOException("Injected write failure");
            }
        };
        Logic logic = logic(model, new JsonStudentDirectoryStorage(file, failingWriter));
        assertThrows(CommandException.class, () -> logic.execute("add n/Alex p/81234567 a/A"));
        assertFalse(Files.exists(file));
        assertTrue(model.getStudentDirectory().getStudentList().isEmpty());
        assertNull(model.getSelectedStudentId());
    }

    @Test
    public void readOnlyAndEquivalentEdit_doNotSave() throws Exception {
        ModelManager model = existingModel();
        Student first = model.getStudentDirectory().getStudentList().getFirst();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json")) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) {
                throw new AssertionError("Unchanged data must not be saved");
            }
        };
        Logic logic = logic(model, storage);
        logic.execute("help");
        assertEquals(first.getId(), model.getSelectedStudentId());
        logic.execute("edit 1 n/Existing Student");
        assertEquals(first.getId(), model.getStudentDirectory().getStudentList().getFirst().getId());
    }

    private ModelManager existingModel() {
        ModelManager model = new ModelManager();
        Student first = new Student(new Name("Existing Student"), new Phone("91234567"), new Address("8 Jalan Besar"));
        model.addStudent(first);
        model.updateFilteredStudentList(student -> student.getId().equals(first.getId()));
        model.selectStudent(first.getId());
        return model;
    }

    private Logic logic(ModelManager model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
