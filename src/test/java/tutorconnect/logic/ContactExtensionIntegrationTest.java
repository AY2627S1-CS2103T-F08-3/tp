package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import tutorconnect.logic.commands.EditCommand;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** Protects the optional-field extension contract through complete contact command transactions. */
public class ContactExtensionIntegrationTest {
    @TempDir
    private Path directory;
    private ModelManager model;
    private Logic logic;
    private JsonStudentDirectoryStorage storage;
    private RecordingWriter writer;
    private Student hidden;
    private Student target;

    @BeforeEach
    public void setUp() throws Exception {
        StudentFields futureFields = new StudentFields(Map.of("futureContactData", new ObjectMapper().readTree(
                "{\"history\":[{\"text\":\"keep me\",\"score\":85},null],\"enabled\":true}")));
        model = new ModelManager();
        hidden = new StudentBuilder().withName("Alex Tan").withPhone("81234567").build();
        target = new StudentBuilder().withName("Nur Aisyah").withPhone("92345678")
                .build().withStudentFields(futureFields);
        model.addStudent(hidden);
        model.addStudent(target);
        writer = new RecordingWriter();
        storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"), writer);
        storage.saveStudentDirectory(model.getStudentDirectory());
        writer.writes = 0;
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        logic.execute("find Nur");
        model.selectStudent(target.getId());
    }

    @Test
    public void contactUpdates_preserveNestedUnknownFieldsAndIdentityAfterRestart() throws Exception {
        logic.execute("guardian 1 g/91234567");
        logic.execute("edit 1 n/Nur Updated a/12/3 New Street");
        assertEquals(2, writer.writes);
        Student updated = model.getStudentDirectory().getStudentList().getLast();
        assertEquals(target.getId(), updated.getId());
        assertEquals(target.getId(), model.getSelectedStudentId());
        assertEquals("Nur Updated", updated.getName().fullName);
        assertEquals("+6591234567", updated.getGuardianPhone().orElseThrow().value);
        assertEquals(target.getStudentFields().toStorage().get("futureContactData"),
                updated.getStudentFields().toStorage().get("futureContactData"));
        assertSame(hidden, model.getStudentDirectory().getStudentList().getFirst());

        // Adding a third profile must also persist the existing opaque envelope intact.
        logic.execute("add n/Zoe Lim p/83456789 a/5 Test Road");
        assertEquals(3, writer.writes);
        ModelManager restarted = new ModelManager(storage.readStudentDirectory().orElseThrow(), new UserPrefs());
        assertEquals(model.getStudentDirectory(), restarted.getStudentDirectory());
        assertEquals(List.of(hidden.getId(), target.getId()), restarted.getStudentDirectory().getStudentList()
                .stream().limit(2).map(Student::getId).toList());
        assertEquals(updated.getStudentFields(), restarted.getStudentDirectory().getStudentList().get(1)
                .getStudentFields());
    }

    @Test
    public void noChangeInvalidAndDuplicateEdits_preserveOpaqueFieldsFileAndView() throws Exception {
        Snapshot before = snapshot();
        logic.execute("edit 1 n/Nur   Aisyah p/+65-9234-5678");
        assertUnchanged(before);
        assertThrows(ParseException.class, () -> logic.execute("edit 1 p/bad"));
        assertUnchanged(before);
        CommandException duplicate = assertThrows(CommandException.class, () ->
                logic.execute("edit 1 n/Alex Tan p/81234567"));
        assertEquals(EditCommand.MESSAGE_DUPLICATE_STUDENT, duplicate.getMessage());
        assertUnchanged(before);
        assertThrows(CommandException.class, () ->
                logic.execute("add n/Alex Tan p/81234567 a/Another Street"));
        assertUnchanged(before);
        assertEquals(0, writer.writes);
    }

    @Test
    public void failedContactMutations_preserveNestedFieldsAndNeverPublish() throws Exception {
        Snapshot before = snapshot();
        writer.failReplacement = true;
        writer.beforeReplacement = () -> assertUnchanged(before);
        for (String command : List.of("edit 1 n/Nur Updated", "guardian 1 g/91234567", "delete 1",
                "add n/Zoe Lim p/83456789 a/5 Test Road")) {
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());
            assertUnchanged(before);
        }
        assertEquals(4, writer.writes);
        try (var files = Files.list(directory)) {
            assertEquals(List.of(storage.getStudentDirectoryFilePath()), files.toList());
        }
    }

    @Test
    public void deleteFilteredStudent_removesEntireEnvelopeAndKeepsSurvivor() throws Exception {
        assertEquals("Student deleted: Nur Aisyah.", logic.execute("delete 1").getFeedbackToUser());
        assertEquals(List.of(hidden), model.getStudentDirectory().getStudentList());
        assertEquals(List.of(), model.getFilteredStudentList());
        assertEquals(List.of(hidden), storage.readStudentDirectory().orElseThrow().getStudentList());
        assertFalse(Files.readString(storage.getStudentDirectoryFilePath()).contains("futureContactData"));
        logic.execute("list");
        assertEquals(List.of(hidden), model.getFilteredStudentList());
    }

    private Snapshot snapshot() throws IOException {
        return new Snapshot(Files.readString(storage.getStudentDirectoryFilePath()),
                List.copyOf(model.getStudentDirectory().getStudentList()), model.getStudentPredicate(),
                model.getSelectedStudentId());
    }

    private void assertUnchanged(Snapshot before) throws IOException {
        assertEquals(before.disk(), Files.readString(storage.getStudentDirectoryFilePath()));
        assertEquals(before.students(), model.getStudentDirectory().getStudentList());
        assertSame(before.predicate(), model.getStudentPredicate());
        assertEquals(before.selected(), model.getSelectedStudentId());
        assertSame(target, model.getFilteredStudentList().getFirst());
    }

    private record Snapshot(String disk, List<Student> students, Predicate<Student> predicate, UUID selected) {}

    @FunctionalInterface
    private interface StorageCheck {
        void run() throws IOException;
    }

    private static class RecordingWriter extends AtomicJsonFile {
        private int writes;
        private boolean failReplacement;
        private StorageCheck beforeReplacement = () -> {};

        @Override
        public void write(Path destination, String json) throws IOException {
            writes++;
            super.write(destination, json);
        }

        @Override
        protected void replace(Path temporary, Path destination) throws IOException {
            beforeReplacement.run();
            if (failReplacement) {
                throw new IOException("Injected replacement failure");
            }
            super.replace(temporary, destination);
        }
    }
}
