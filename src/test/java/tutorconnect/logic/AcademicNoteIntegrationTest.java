package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.ParserUtil;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.AcademicNote;
import tutorconnect.model.student.Student;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** End-to-end tests for {@code note} and {@code clearnote}: real parsing, commands and JSON persistence. */
public class AcademicNoteIntegrationTest {
    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";
    private static final String SAVE_FAILURE = "Error: Changes could not be saved. No changes were made.";
    private static final List<String> OTHER_FIELD_KEYS =
            List.of("guardianPhone", "hourlyRate", "educationLevel", "subject", "weeklySlot");

    @TempDir
    private Path directory;

    private Path file;
    private final AtomicInteger writes = new AtomicInteger();

    /** One run of the application over the data file, as started from scratch. */
    private final class App {
        private final ModelManager model;
        private final Logic logic;

        private App(ModelManager model, JsonStudentDirectoryStorage storage) {
            this.model = model;
            this.logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        }

        private Student first() {
            return model.getStudentDirectory().getStudentList().getFirst();
        }

        private Student last() {
            return model.getStudentDirectory().getStudentList().getLast();
        }

        private String run(String command) throws Exception {
            return logic.execute(command).getFeedbackToUser();
        }
    }

    private JsonStudentDirectoryStorage countingStorage() {
        return new JsonStudentDirectoryStorage(file, new AtomicJsonFile() {
            @Override
            public void write(Path destination, String json) throws IOException {
                writes.incrementAndGet();
                super.write(destination, json);
            }
        });
    }

    private App start() throws Exception {
        file = directory.resolve("students.json");
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").build());
        model.addStudent(new StudentBuilder().withName("Other Student").withPhone("91234567").build());
        return new App(model, countingStorage());
    }

    private App restart() throws Exception {
        JsonStudentDirectoryStorage storage = countingStorage();
        return new App(new ModelManager(storage.readStudentDirectory().orElseThrow(), new UserPrefs()), storage);
    }

    private App startFromRaw(String json) throws Exception {
        file = directory.resolve("students.json");
        Files.writeString(file, json);
        return restart();
    }

    private JsonNode stored(int position) throws Exception {
        return new ObjectMapper().readTree(Files.readString(file)).path("persons").get(position);
    }

    private static void assertParseError(App app, String command, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> app.logic.execute(command)).getMessage(),
                command);
    }

    private static void assertCommandError(App app, String command, String message) {
        assertEquals(message, assertThrows(CommandException.class, () -> app.logic.execute(command)).getMessage(),
                command);
    }

    @Test
    public void setReplaceNoChangeAndClear_useExactMessagesAndSaveOnlyRealChanges() throws Exception {
        App app = start();
        Student target = app.first();

        assertEquals("Note set for Alex Tan.", app.run("note 1 m/Weak in calculus"));
        assertEquals(target.getId(), app.model.getSelectedStudentId());
        assertEquals("Note for Alex Tan is already up to date.", app.run("note 1 m/  Weak   in calculus "));
        assertEquals("Note updated for Alex Tan.", app.run("note 1 m/weak in calculus"));
        assertEquals("Note for Alex Tan is already up to date.", app.run("note 1 m/weak in calculus"));
        assertEquals("Note cleared for Alex Tan.", app.run("clearnote 1"));
        assertEquals("Alex Tan has no note to clear.", app.run("clearnote 1"));

        assertEquals(3, writes.get(), "only the set, the update and the clear change the file");
        assertEquals("—", AcademicNote.display(app.first().getStudentFields()));
    }

    @Test
    public void note_keepsIdPositionAndEveryOtherFieldBuiltWithRealCommands() throws Exception {
        App app = start();
        app.run("guardian 1 g/91234567");
        app.run("rate 1 r/65");
        app.run("level 1 l/Sec4");
        app.run("subject 1 s/Math/Science");
        app.run("schedule 1 d/Tue t/19:00");
        Student before = app.first();
        Student other = app.last();
        Map<String, JsonNode> fieldsBefore = before.getStudentFields().toStorage();
        for (String key : OTHER_FIELD_KEYS) {
            assertTrue(fieldsBefore.containsKey(key), "the real command should have written " + key);
        }
        JsonNode storedBefore = stored(0);

        for (String command : List.of("note 1 m/Weak in calculus", "note 1 m/Short attention span",
                "note 1 m/Short attention span", "clearnote 1", "note 1 m/Back again")) {
            app.run(command);

            Student after = app.first();
            assertEquals(before.getId(), after.getId(), command);
            assertSame(other, app.last(), command);
            assertEquals(before.getName(), after.getName());
            assertEquals(before.getPhone(), after.getPhone());
            assertEquals(before.getAddress(), after.getAddress());
            for (String key : OTHER_FIELD_KEYS) {
                assertEquals(fieldsBefore.get(key), after.getStudentFields().toStorage().get(key), key);
                assertEquals(storedBefore.get(key), stored(0).get(key), "stored " + key);
            }
        }
        assertEquals("Back again", stored(0).get("academicNote").asText());
        assertEquals(1, Files.readString(file).split("\"academicNote\"", -1).length - 1);
        assertEquals(app.first(), restart().first());
    }

    @Test
    public void otherCommandsAfterANote_keepTheNote() throws Exception {
        App app = start();
        app.run("note 1 m/Weak in calculus");

        app.run("guardian 1 g/91234567");
        app.run("rate 1 r/65");
        app.run("level 1 l/Sec4");
        app.run("subject 1 s/Math");
        app.run("schedule 1 d/Tue t/19:00");
        app.run("edit 1 a/22 New Street");

        assertEquals("Weak in calculus", AcademicNote.display(app.first().getStudentFields()));
        assertEquals("Weak in calculus", stored(0).get("academicNote").asText());
        assertEquals("Weak in calculus", AcademicNote.display(restart().first().getStudentFields()));
    }

    @Test
    public void restart_equivalentNoteIsNoChangeAndADifferentNoteReplacesIt() throws Exception {
        App app = start();
        app.run("note 1 m/Weak in calculus");
        writes.set(0);

        App restarted = restart();
        assertEquals("Weak in calculus", AcademicNote.display(restarted.first().getStudentFields()));
        assertEquals("Note for Alex Tan is already up to date.", restarted.run("note 1 m/Weak in calculus"));
        assertEquals(0, writes.get(), "an equivalent note must not write the file");
        assertNull(restarted.model.getSelectedStudentId());

        assertEquals("Note updated for Alex Tan.", restarted.run("note 1 m/Needs more practice"));
        assertEquals(1, writes.get());
        assertEquals("Needs more practice", AcademicNote.display(restart().first().getStudentFields()));
    }

    @Test
    public void filesWithoutANote_loadUnsetAndAcceptOneKeepingTheLegacyData() throws Exception {
        App legacy = startFromRaw("""
                {"persons":[{"name":"Legacy Lee","phone":"91234567","email":"legacy@example.com",
                "address":"1 Old Road","tags":["friends"],"guardianPhone":"+6591234567","hourlyRate":"40"}]}
                """);
        Student loaded = legacy.first();
        assertEquals("—", AcademicNote.display(loaded.getStudentFields()));
        assertEquals("Legacy Lee has no note to clear.", legacy.run("clearnote 1"));
        assertEquals(0, writes.get());

        assertEquals("Note set for Legacy Lee.", legacy.run("note 1 m/Prefers visuals"));

        Student updated = legacy.first();
        assertEquals(loaded.getId(), updated.getId());
        assertEquals("legacy@example.com", stored(0).get("email").asText());
        assertEquals("friends", stored(0).get("tags").get(0).asText());
        assertEquals("+6591234567", stored(0).get("guardianPhone").asText());
        assertEquals("Prefers visuals", stored(0).get("academicNote").asText());
        assertEquals(updated, restart().first());
    }

    @Test
    public void unreadableStoredNote_isTreatedAsUnsetByNoteAndRemovedByClearnote() throws Exception {
        App app = startFromRaw("""
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street","academicNote":5}]}
                """);
        assertEquals("—", AcademicNote.display(app.first().getStudentFields()));
        assertEquals("Note set for Alex Tan.", app.run("note 1 m/Readable now"));
        assertEquals("Readable now", stored(0).get("academicNote").asText());

        App other = startFromRaw("""
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street","academicNote":5}]}
                """);
        assertEquals("Note cleared for Alex Tan.", other.run("clearnote 1"));
        assertFalse(stored(0).has("academicNote"));
    }

    @Test
    public void unrelatedFutureFields_surviveNoteAndClearnote() throws Exception {
        App app = startFromRaw("""
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street",
                "futureField":"keep me","anotherFutureField":{"nested":[1,2,3]}}]}
                """);

        app.run("note 1 m/Weak in calculus");
        app.run("clearnote 1");

        assertEquals("keep me", stored(0).get("futureField").asText());
        assertEquals(3, stored(0).get("anotherFutureField").get("nested").size());
        assertFalse(stored(0).has("academicNote"));
    }

    @Test
    public void clear_removesTheKeyFromTheFileAndStaysClearedAfterARestart() throws Exception {
        App app = start();
        app.run("note 1 m/Weak in calculus");
        assertTrue(Files.readString(file).contains("academicNote"));

        app.run("clearnote 1");

        assertFalse(Files.readString(file).contains("academicNote"));
        assertEquals("—", AcademicNote.display(restart().first().getStudentFields()));
    }

    @Test
    public void saveFailure_preservesFileModelFilterAndSelection() throws Exception {
        for (String command : List.of("note 1 m/New note", "clearnote 1")) {
            App setup = start();
            setup.run("note 1 m/Existing note");
            Student target = setup.first();
            setup.model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
            setup.model.selectStudent(target.getId());
            var predicate = setup.model.getStudentPredicate();
            List<Student> before = List.copyOf(setup.model.getStudentDirectory().getStudentList());
            String saved = Files.readString(file);
            AtomicJsonFile failingWriter = new AtomicJsonFile() {
                @Override
                protected void replace(Path temporary, Path destination) throws IOException {
                    throw new IOException("Injected atomic replacement failure");
                }
            };
            Logic logic = new LogicManager(setup.model, new StorageManager(
                    new JsonStudentDirectoryStorage(file, failingWriter),
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));

            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));

            assertEquals(SAVE_FAILURE, failure.getMessage(), command);
            assertEquals(saved, Files.readString(file), command);
            assertEquals(before, setup.model.getStudentDirectory().getStudentList(), command);
            assertSame(target, setup.model.getFilteredStudentList().getFirst(), command);
            assertSame(predicate, setup.model.getStudentPredicate(), command);
            assertEquals(target.getId(), setup.model.getSelectedStudentId(), command);
        }
    }

    @Test
    public void errors_useExactMessagesAndLeaveStateUntouched() throws Exception {
        App app = start();
        app.run("note 1 m/Existing note");
        app.model.selectStudent(app.last().getId());
        List<Student> before = List.copyOf(app.model.getStudentDirectory().getStudentList());
        writes.set(0);

        assertParseError(app, "note 1 m/", AcademicNote.MESSAGE_CONSTRAINTS);
        assertParseError(app, "note 1 m/" + "a".repeat(501), AcademicNote.MESSAGE_CONSTRAINTS);
        assertParseError(app, "note 1", "Error: Missing required parameter: m/NOTE.");
        assertParseError(app, "note 1 foo m/x", "Error: Unexpected text after command.");
        assertParseError(app, "note 1 x/y m/x", "Error: Unknown parameter: x/.");
        assertParseError(app, "note 0 m/x", INVALID_INDEX);
        assertParseError(app, "note m/x", ParserUtil.MESSAGE_MISSING_INDEX);
        assertParseError(app, "clearnote", "Error: Missing required parameter: INDEX.");
        assertParseError(app, "clearnote 1 2", "Error: Unexpected text after command.");
        assertParseError(app, "clearnote 0", INVALID_INDEX);
        // The note is validated before the row is looked up, so a bad note wins over a missing student.
        assertParseError(app, "note 99 m/", AcademicNote.MESSAGE_CONSTRAINTS);
        assertCommandError(app, "note 99 m/x", "Error: No student exists at index 99.");
        assertCommandError(app, "clearnote 99", "Error: No student exists at index 99.");
        String huge = "9".repeat(100);
        assertCommandError(app, "note " + huge + " m/x", "Error: No student exists at index " + huge + ".");
        assertCommandError(app, "clearnote " + huge, "Error: No student exists at index " + huge + ".");

        assertEquals(before, app.model.getStudentDirectory().getStudentList());
        assertEquals(app.last().getId(), app.model.getSelectedStudentId());
        assertEquals(0, writes.get());
    }

    @Test
    public void note_resolvesTheDisplayedListAndManyStudentsMayShareTheSameNote() throws Exception {
        App app = start();
        Student second = app.last();
        app.model.updateFilteredStudentList(student -> student.getId().equals(second.getId()));

        assertEquals("Note set for Other Student.", app.run("note 1 m/Needs revision"));
        app.model.updateFilteredStudentList(student -> true);
        assertEquals("Note set for Alex Tan.", app.run("note 1 m/Needs revision"));

        for (Student student : app.model.getStudentDirectory().getStudentList()) {
            assertEquals("Needs revision", AcademicNote.display(student.getStudentFields()));
        }
    }

    @Test
    public void deletingAStudent_removesTheirNoteFromTheFile() throws Exception {
        App app = start();
        app.run("note 1 m/Weak in calculus");
        assertTrue(Files.readString(file).contains("academicNote"));

        app.run("delete 1");

        assertFalse(Files.readString(file).contains("academicNote"));
        assertEquals("Other Student", restart().first().getName().toString());
    }

    @Test
    public void punctuationSlashesAndNormalization_surviveTheWholeStack() throws Exception {
        App app = start();
        String note = "Prefers visuals and/or diagrams; he/she works w/ parent at 50% (p.4)! Speed in m/s.";

        app.run("note 1 m/" + note);

        assertEquals(note, AcademicNote.display(app.first().getStudentFields()));
        assertEquals(note, stored(0).get("academicNote").asText());
        assertEquals(note, AcademicNote.display(restart().first().getStudentFields()));

        app.run("note 1 m/Needs x² practice");
        assertEquals("Needs x2 practice", AcademicNote.display(app.first().getStudentFields()));
    }

    @Test
    public void longestNote_isAcceptedAndPersistedAndOneMoreCharacterIsRejected() throws Exception {
        App app = start();
        String longest = "a".repeat(500);

        assertEquals("Note set for Alex Tan.", app.run("note 1 m/" + longest));
        assertParseError(app, "note 1 m/" + longest + "b", AcademicNote.MESSAGE_CONSTRAINTS);

        assertEquals(longest, restart().first().getStudentFields().display("academicNote"));
    }
}
