package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.Subject;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** Remaining F06 acceptance cases across fresh models and real atomic file replacement. */
public class SubjectLifecycleTest {
    @TempDir
    private Path directory;

    @Test
    public void restart_noChangeAndReplacement_preserveCompleteProfileAndOtherStudents() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").withTags("legacy").build()
                .withStudentFields(new StudentFields(Map.of("futureField",
                        new ObjectMapper().readTree("{\"nested\":[1,\"keep\"]}")))));
        model.addStudent(new StudentBuilder().withName("Other Student").build());
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        Logic setup = logic(model, storage);
        for (String command : List.of("guardian 1 g/91234567", "rate 1 r/40.25", "level 1 l/Sec4",
                "schedule 1 d/Mon t/16:00", "subject 1 s/H2 Math")) {
            setup.execute(command);
        }
        Student complete = model.getStudentDirectory().getStudentList().getFirst();
        ModelManager restarted = restart(storage);
        Student target = restarted.getStudentDirectory().getStudentList().getFirst();
        Student other = restarted.getStudentDirectory().getStudentList().getLast();
        assertEquals(complete, target);
        restarted.selectStudent(other.getId());
        byte[] before = Files.readAllBytes(storage.getStudentDirectoryFilePath());
        AtomicInteger writes = new AtomicInteger();
        JsonStudentDirectoryStorage counted = new JsonStudentDirectoryStorage(storage.getStudentDirectoryFilePath()) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory state) throws IOException {
                writes.incrementAndGet();
                super.saveStudentDirectory(state);
            }
        };
        Logic logic = logic(restarted, counted);

        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/\u00a0h2\u2002MATH ").getFeedbackToUser());
        assertEquals(0, writes.get());
        assertArrayEquals(before, Files.readAllBytes(storage.getStudentDirectoryFilePath()));
        assertSame(target, restarted.getStudentDirectory().getStudentList().getFirst());
        assertEquals(other.getId(), restarted.getSelectedStudentId());

        assertEquals("Subject updated for Alex Tan: H2 Math -> English Language.",
                logic.execute("subject 1 s/English Language").getFeedbackToUser());
        Student updated = restarted.getStudentDirectory().getStudentList().getFirst();
        assertEquals(complete.withStudentFields(complete.getStudentFields()
                .with(Subject.FIELD, new Subject("English Language"))), updated);
        assertSame(other, restarted.getStudentDirectory().getStudentList().getLast());
        assertEquals(target.getId(), restarted.getSelectedStudentId());
        assertEquals(1, writes.get());
        assertEquals(restarted.getStudentDirectory(), restart(storage).getStudentDirectory());

        logic.execute("level 1 l/JC1");
        logic.execute("schedule 1 d/Fri t/09:30");
        assertEquals("English Language", restart(storage).getStudentDirectory().getStudentList().getFirst()
                .getStudentFields().get(Subject.FIELD).orElseThrow().value);
    }

    @Test
    public void legacyMissingAndNullSubject_canBeAssignedWithoutLosingLegacyData() throws Exception {
        for (String subjectProperty : List.of("", ",\"subject\":null")) {
            Path file = directory.resolve("legacy.json");
            Files.writeString(file, """
                    {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Clementi Ave",
                    "email":"alex@example.com","tags":["legacy"],"guardianPhone":"+6591234567",
                    "hourlyRate":"40.25","futureField":{"nested":[1,"keep"]}%s}]}
                    """.formatted(subjectProperty));
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
            ModelManager model = restart(storage);
            Student original = model.getStudentDirectory().getStudentList().getFirst();
            assertTrue(original.getStudentFields().get(Subject.FIELD).isEmpty());
            assertEquals(original.getId(), restart(storage).getStudentDirectory().getStudentList().getFirst().getId());

            assertEquals("Subject set for Alex Tan: Physics & Chemistry.",
                    logic(model, storage).execute("subject 1 s/Physics & Chemistry").getFeedbackToUser());

            Student loaded = restart(storage).getStudentDirectory().getStudentList().getFirst();
            assertEquals(original.withStudentFields(original.getStudentFields()
                    .with(Subject.FIELD, new Subject("Physics & Chemistry"))), loaded);
            assertTrue(Files.readString(file).contains("\"id\""));
        }
    }

    @Test
    public void handEditedSubject_normalizesOnReadAndUnreadableValuesCanBeReplaced() throws Exception {
        for (String stored : List.of("\"  Ｈ２   Math  \"", "\"@@\"", "12", "{\"value\":\"Math\"}")) {
            Path file = directory.resolve("edited.json");
            Files.writeString(file, """
                    {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Clementi Ave",
                    "subject":%s,"futureField":{"keep":true}}]}
                    """.formatted(stored));
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
            ModelManager model = restart(storage);
            Student original = model.getStudentDirectory().getStudentList().getFirst();
            Logic logic = logic(model, storage);
            if (stored.contains("Ｈ２")) {
                byte[] bytes = Files.readAllBytes(file);
                assertEquals("H2 Math", original.getStudentFields().get(Subject.FIELD).orElseThrow().value);
                assertEquals("Subject for Alex Tan is already H2 Math.",
                        logic.execute("subject 1 s/h2 math").getFeedbackToUser());
                assertArrayEquals(bytes, Files.readAllBytes(file));
                assertSame(original, model.getStudentDirectory().getStudentList().getFirst());
            } else {
                assertThrows(IllegalArgumentException.class, () -> original.getStudentFields().get(Subject.FIELD));
            }
            String expected = stored.contains("Ｈ２")
                    ? "Subject updated for Alex Tan: H2 Math -> English Language."
                    : "Subject set for Alex Tan: English Language.";
            assertEquals(expected, logic.execute("subject 1 s/English Language").getFeedbackToUser());
            assertEquals(original.withStudentFields(original.getStudentFields()
                    .with(Subject.FIELD, new Subject("English Language"))),
                    restart(storage).getStudentDirectory().getStudentList().getFirst());
        }
    }

    @Test
    public void failedReplacement_filteredStudent_preservesStateAndCleansTemporaryFile() throws Exception {
        ModelManager model = new ModelManager();
        Student first = new StudentBuilder().withName("First Student").build();
        Student target = new StudentBuilder().withName("Alex Tan").build().withStudentFields(
                new StudentFields().with(Subject.FIELD, new Subject("H2 Math")));
        model.addStudent(first);
        model.addStudent(target);
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        model.selectStudent(target.getId());
        var predicate = model.getStudentPredicate();
        Path file = directory.resolve("students.json");
        new JsonStudentDirectoryStorage(file).saveStudentDirectory(model.getStudentDirectory());
        byte[] before = Files.readAllBytes(file);
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path destination) throws IOException {
                assertTrue(Files.readString(temporary).contains("English Language"));
                throw new IOException("Injected F06 atomic replacement failure");
            }
        };
        Logic logic = logic(model, new JsonStudentDirectoryStorage(file, writer));

        CommandException failure = assertThrows(CommandException.class, () -> {
            logic.execute("subject 1 s/English Language");
        });
        assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());

        assertArrayEquals(before, Files.readAllBytes(file));
        assertSame(first, model.getStudentDirectory().getStudentList().getFirst());
        assertSame(target, model.getStudentDirectory().getStudentList().getLast());
        assertSame(target, model.getFilteredStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(target.getId(), model.getSelectedStudentId());
        try (var files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    private ModelManager restart(JsonStudentDirectoryStorage storage) throws Exception {
        return new ModelManager(storage.readStudentDirectory().orElseThrow(), new UserPrefs());
    }

    private Logic logic(ModelManager model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
