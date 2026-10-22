package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.AssessmentRecord;
import tutorconnect.model.student.Student;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** End-to-end tests for {@code assess}, {@code assessments} and {@code unassess} over real JSON persistence. */
public class AssessmentHistoryIntegrationTest {
    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";
    private static final String SAVE_FAILURE = "Error: Changes could not be saved. No changes were made.";
    private static final String UNREADABLE = "Error: The stored assessment history for Alex Tan could not be read. "
            + "Correct or remove the \"assessments\" entry in the data file.";
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

        private List<String> lines() {
            return AssessmentHistory.detailLines(first().getStudentFields());
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
    public void addListAndRemove_useExactMessagesNumbersAndSaveOnlyRealChanges() throws Exception {
        App app = start();
        Student target = app.first();

        assertEquals("Assessment added for Alex Tan: #1 2026-10-05 Later test: B+.",
                app.run("assess 1 d/2026-10-05 t/Later test g/B+"));
        assertEquals(target.getId(), app.model.getSelectedStudentId());
        assertEquals("Assessment added for Alex Tan: #1 2026-09-01 Earlier test: 85/100.",
                app.run("assess 1 g/85/100 d/2026-09-01 t/Earlier test"));
        assertEquals("Assessments for Alex Tan:\n1. 2026-09-01 Earlier test: 85/100\n2. 2026-10-05 Later test: B+",
                app.run("assessments 1"));
        assertEquals(2, writes.get(), "listing records never writes the file");

        assertEquals("Assessment removed from Alex Tan: 2026-09-01 Earlier test: 85/100.", app.run("unassess 1 i/1"));
        assertEquals(3, writes.get());
        assertEquals("Assessment removed from Alex Tan: 2026-10-05 Later test: B+.", app.run("unassess 1 i/1"));
        assertEquals(4, writes.get());
        assertEquals("Alex Tan has no assessment records.", app.run("assessments 1"));
        assertEquals(4, writes.get());
        assertFalse(stored(0).has("assessments"), "an empty history is not stored");
        assertEquals(List.of("Assessments: none recorded"), app.lines());
    }

    @Test
    public void records_areStoredInDisplayOrderWithTiesInTheOrderTheyWereAdded() throws Exception {
        App app = start();
        app.run("assess 1 d/2026-10-05 t/Third g/C");
        app.run("assess 1 d/2026-09-01 t/First g/A");
        app.run("assess 1 d/2026-10-05 t/Fourth g/D");
        app.run("assess 1 d/2026-09-30 t/Second g/B");

        JsonNode records = stored(0).get("assessments");

        assertEquals(4, records.size());
        assertEquals(List.of("First", "Second", "Third", "Fourth"),
                List.of(records.get(0).get("title").asText(), records.get(1).get("title").asText(),
                        records.get(2).get("title").asText(), records.get(3).get("title").asText()));
        assertEquals("2026-09-01", records.get(0).get("date").asText());
        assertEquals("A", records.get(0).get("grade").asText());
        assertEquals(3, records.get(0).size());
        assertEquals(List.of("Assessments (4):", "1. 2026-09-01 First: A", "2. 2026-09-30 Second: B",
                "3. 2026-10-05 Third: C", "4. 2026-10-05 Fourth: D"), app.lines());
    }

    @Test
    public void gradesAreStoredAsTypedAndNeverInterpretedAsNumbers() throws Exception {
        App app = start();
        app.run("assess 1 d/2026-10-01 t/Singapore exam g/A1");
        app.run("assess 1 d/2026-10-01 t/UK mock g/Grade 7");
        app.run("assess 1 d/2026-10-01 t/Class quiz g/85/100");
        app.run("assess 1 d/2026-10-01 t/Percent test g/72.5%");
        app.run("assess 1 d/2026-10-01 t/Plus minus g/B+");
        app.run("assess 1 d/2026-10-01 t/Pass fail g/Pass");

        List<String> grades = new ArrayList<>();
        for (JsonNode record : stored(0).get("assessments")) {
            grades.add(record.get("grade").asText());
        }
        assertEquals(List.of("A1", "Grade 7", "85/100", "72.5%", "B+", "Pass"), grades);
    }

    @Test
    public void records_keepIdPositionAndEveryOtherFieldBuiltWithRealCommands() throws Exception {
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

        for (String command : List.of("assess 1 d/2026-10-01 t/Midterm g/85/100", "assess 1 d/2026-12-01 t/Final g/A1",
                "unassess 1 i/1", "unassess 1 i/1", "assess 1 d/2026-01-01 t/Back again g/B")) {
            app.run(command);

            Student after = app.first();
            assertEquals(before.getId(), after.getId(), command);
            assertSame(other, app.last(), command);
            for (String key : OTHER_FIELD_KEYS) {
                assertEquals(fieldsBefore.get(key), after.getStudentFields().toStorage().get(key), key);
                assertEquals(storedBefore.get(key), stored(0).get(key), "stored " + key);
            }
        }
        assertEquals(1, stored(0).get("assessments").size());
        assertEquals(app.first(), restart().first());
    }

    @Test
    public void otherCommandsAfterRecords_keepTheHistory() throws Exception {
        App app = start();
        app.run("assess 1 d/2026-10-01 t/Midterm g/85/100");
        app.run("assess 1 d/2026-12-01 t/Final g/A1");
        List<String> before = app.lines();

        app.run("guardian 1 g/91234567");
        app.run("rate 1 r/65");
        app.run("level 1 l/Sec4");
        app.run("subject 1 s/Math");
        app.run("schedule 1 d/Tue t/19:00");
        app.run("edit 1 a/22 New Street");

        assertEquals(before, app.lines());
        assertEquals(before, restart().lines());
    }

    @Test
    public void restart_keepsTheRecordsAndStillRejectsADuplicate() throws Exception {
        App app = start();
        app.run("assess 1 d/2026-10-01 t/Math Midterm g/85/100");
        app.run("assess 1 d/2026-09-01 t/Quiz g/7/10");
        writes.set(0);

        App restarted = restart();
        assertEquals(List.of("Assessments (2):", "1. 2026-09-01 Quiz: 7/10", "2. 2026-10-01 Math Midterm: 85/100"),
                restarted.lines());
        assertEquals("Assessments for Alex Tan:\n1. 2026-09-01 Quiz: 7/10\n2. 2026-10-01 Math Midterm: 85/100",
                restarted.run("assessments 1"));
        assertCommandError(restarted, "assess 1 d/2026-10-01 t/  math   MIDTERM g/A1",
                "Error: Alex Tan already has an assessment \"Math Midterm\" on 2026-10-01. "
                        + "Remove it first to change it.");
        assertEquals(0, writes.get());
        assertEquals("Assessment removed from Alex Tan: 2026-10-01 Math Midterm: 85/100.",
                restarted.run("unassess 1 i/2"));
        assertEquals(List.of("Assessments (1):", "1. 2026-09-01 Quiz: 7/10"), restart().lines());
    }

    @Test
    public void filesWithoutRecords_loadEmptyAndAcceptOneKeepingTheLegacyData() throws Exception {
        App legacy = startFromRaw("""
                {"persons":[{"name":"Legacy Lee","phone":"91234567","email":"legacy@example.com",
                "address":"1 Old Road","tags":["friends"],"guardianPhone":"+6591234567","hourlyRate":"40",
                "futureField":"keep me"}]}
                """);
        Student loaded = legacy.first();
        assertEquals(List.of("Assessments: none recorded"), legacy.lines());
        assertEquals("Legacy Lee has no assessment records.", legacy.run("assessments 1"));
        assertEquals(0, writes.get());

        assertEquals("Assessment added for Legacy Lee: #1 2026-10-01 Quiz: A.",
                legacy.run("assess 1 d/2026-10-01 t/Quiz g/A"));

        assertEquals(loaded.getId(), legacy.first().getId());
        assertEquals("legacy@example.com", stored(0).get("email").asText());
        assertEquals("friends", stored(0).get("tags").get(0).asText());
        assertEquals("+6591234567", stored(0).get("guardianPhone").asText());
        assertEquals("keep me", stored(0).get("futureField").asText());
        assertEquals(legacy.first(), restart().first());
    }

    @Test
    public void unreadableStoredHistory_isReportedByEveryCommandAndTheFileIsNeverOverwritten() throws Exception {
        App app = startFromRaw("""
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street",
                "assessments":[{"date":"2026-02-30","title":"Broken","grade":"A"}]}]}
                """);
        byte[] before = Files.readAllBytes(file);

        assertEquals(List.of("Assessments: stored data could not be read"), app.lines());
        assertCommandError(app, "assess 1 d/2026-10-01 t/Quiz g/A", UNREADABLE);
        assertCommandError(app, "assessments 1", UNREADABLE);
        assertCommandError(app, "unassess 1 i/1", UNREADABLE);

        assertEquals(0, writes.get());
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    public void unrelatedFutureFields_surviveEveryAssessmentCommand() throws Exception {
        App app = startFromRaw("""
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street",
                "futureField":"keep me","anotherFutureField":{"nested":[1,2,3]}}]}
                """);

        app.run("assess 1 d/2026-10-01 t/Quiz g/A");
        app.run("unassess 1 i/1");

        assertEquals("keep me", stored(0).get("futureField").asText());
        assertEquals(3, stored(0).get("anotherFutureField").get("nested").size());
        assertFalse(stored(0).has("assessments"));
    }

    @Test
    public void saveFailure_preservesFileModelFilterAndSelection() throws Exception {
        for (String command : List.of("assess 1 d/2026-12-01 t/New g/A", "unassess 1 i/1")) {
            App setup = start();
            setup.run("assess 1 d/2026-10-01 t/Existing g/B");
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
        app.run("assess 1 d/2026-10-01 t/Existing g/B");
        app.model.selectStudent(app.last().getId());
        List<Student> before = List.copyOf(app.model.getStudentDirectory().getStudentList());
        writes.set(0);

        assertParseError(app, "assess 1 d/2026-02-30 t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
        assertParseError(app, "assess 1 d/2026-2-3 t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
        assertParseError(app, "assess 1 d/2026-10-01 t/Quiz! g/A", AssessmentRecord.MESSAGE_INVALID_TITLE);
        assertParseError(app, "assess 1 d/2026-10-01 t/" + "a".repeat(61) + " g/A",
                AssessmentRecord.MESSAGE_INVALID_TITLE);
        assertParseError(app, "assess 1 d/2026-10-01 t/Quiz g/A*", AssessmentRecord.MESSAGE_INVALID_GRADE);
        assertParseError(app, "assess 1 d/2026-10-01 t/Quiz g/" + "a".repeat(21),
                AssessmentRecord.MESSAGE_INVALID_GRADE);
        assertParseError(app, "assess 1 t/Quiz g/A", "Error: Missing required parameter: d/DATE.");
        assertParseError(app, "assess 1 d/2026-10-01 t/Quiz g/A g/B",
                "Error: Parameter g/ may be specified only once.");
        assertParseError(app, "assess 1 x/1 d/2026-10-01 t/Quiz g/A", "Error: Unknown parameter: x/.");
        assertParseError(app, "assess 1 foo d/2026-10-01 t/Quiz g/A", "Error: Unexpected text after command.");
        assertParseError(app, "assess 0 d/2026-10-01 t/Quiz g/A", INVALID_INDEX);
        assertParseError(app, "assessments", "Error: Missing required parameter: INDEX.");
        assertParseError(app, "assessments 1 2", "Error: Unexpected text after command.");
        assertParseError(app, "assessments 0", INVALID_INDEX);
        assertParseError(app, "unassess 1", "Error: Missing required parameter: i/RECORD.");
        assertParseError(app, "unassess 1 i/0", "Error: Invalid record number. "
                + "Enter a positive whole number without leading zeroes.");
        assertParseError(app, "unassess 0 i/1", INVALID_INDEX);
        // Values are validated before the row is looked up, so a bad value wins over a missing student.
        assertParseError(app, "assess 99 d/bad t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
        assertCommandError(app, "assess 99 d/2026-10-01 t/Quiz g/A", "Error: No student exists at index 99.");
        assertCommandError(app, "assessments 99", "Error: No student exists at index 99.");
        assertCommandError(app, "unassess 99 i/1", "Error: No student exists at index 99.");
        assertCommandError(app, "unassess 1 i/2", "Error: No assessment record 2 for Alex Tan.");
        assertCommandError(app, "unassess 2 i/1", "Error: No assessment record 1 for Other Student.");
        String huge = "9".repeat(100);
        assertCommandError(app, "assess " + huge + " d/2026-10-01 t/Quiz g/A",
                "Error: No student exists at index " + huge + ".");
        assertCommandError(app, "unassess 1 i/" + huge, "Error: No assessment record " + huge + " for Alex Tan.");
        assertCommandError(app, "assess 1 d/2026-10-01 t/EXISTING g/A",
                "Error: Alex Tan already has an assessment \"Existing\" on 2026-10-01. Remove it first to change it.");

        assertEquals(before, app.model.getStudentDirectory().getStudentList());
        assertEquals(app.last().getId(), app.model.getSelectedStudentId());
        assertEquals(0, writes.get());
    }

    @Test
    public void commands_resolveTheDisplayedListAndManyStudentsMayShareTheSameAssessment() throws Exception {
        App app = start();
        Student second = app.last();
        app.model.updateFilteredStudentList(student -> student.getId().equals(second.getId()));

        assertEquals("Assessment added for Other Student: #1 2026-10-01 Math Midterm: A1.",
                app.run("assess 1 d/2026-10-01 t/Math Midterm g/A1"));
        app.model.updateFilteredStudentList(student -> true);
        assertEquals("Assessment added for Alex Tan: #1 2026-10-01 Math Midterm: A1.",
                app.run("assess 1 d/2026-10-01 t/Math Midterm g/A1"));

        assertEquals(1, AssessmentHistory.of(app.first().getStudentFields()).size());
        assertEquals(1, AssessmentHistory.of(app.last().getStudentFields()).size());
    }

    @Test
    public void deletingAStudent_removesTheirRecordsFromTheFile() throws Exception {
        App app = start();
        app.run("assess 1 d/2026-10-01 t/Quiz g/A");
        assertTrue(Files.readString(file).contains("\"assessments\""));

        app.run("delete 1");

        assertFalse(Files.readString(file).contains("\"assessments\""));
        assertEquals("Other Student", restart().first().getName().toString());
    }

    @Test
    public void aHundredRecordsAreAcceptedAndTheNextOneIsRejected() throws Exception {
        App app = start();
        for (int i = 1; i <= 100; i++) {
            app.run("assess 1 d/2026-01-01 t/Test " + i + " g/A");
        }
        assertEquals(100, stored(0).get("assessments").size());
        int writesBefore = writes.get();

        assertCommandError(app, "assess 1 d/2026-02-01 t/One more g/A",
                "Error: A student can have at most 100 assessment records.");

        assertEquals(writesBefore, writes.get());
        assertEquals(101, app.lines().size(), "a count line and one line per record");
        assertEquals("Assessments (100):", app.lines().getFirst());
        assertEquals(100, restart().model.getStudentDirectory().getStudentList().getFirst().getStudentFields()
                .toStorage().get("assessments").size());
    }
}
