package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tutorconnect.model.ModelManager;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/**
 * F05: setting the education level must keep every other optional field exactly as the real commands wrote it,
 * and the other commands must keep the level. The fields are built with real commands, not raw JSON placeholders.
 */
public class LevelPreservationIntegrationTest {
    private static final List<String> OTHER_FIELD_KEYS =
            List.of("guardianPhone", "hourlyRate", "subject", "weeklySlot");

    @TempDir
    private Path directory;

    private JsonStudentDirectoryStorage storage;
    private ModelManager model;
    private Logic logic;

    private void start() throws Exception {
        model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").build());
        model.addStudent(new StudentBuilder().withName("Other Student").withPhone("91234567").build());
        storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    private void setUpOtherFieldsWithRealCommands() throws Exception {
        logic.execute("guardian 1 g/91234567");
        logic.execute("rate 1 r/65");
        logic.execute("subject 1 s/Math/Science");
        logic.execute("schedule 1 d/Tue t/19:00");
    }

    private Student first() {
        return model.getStudentDirectory().getStudentList().getFirst();
    }

    private JsonNode storedStudent(int position) throws Exception {
        return new ObjectMapper().readTree(Files.readString(storage.getStudentDirectoryFilePath()))
                .path("persons").get(position);
    }

    @Test
    public void level_setReplaceAndNoChange_keepGuardianRateSubjectScheduleAndIdentity() throws Exception {
        start();
        setUpOtherFieldsWithRealCommands();
        Student before = first();
        Student other = model.getStudentDirectory().getStudentList().getLast();
        Map<String, JsonNode> fieldsBefore = before.getStudentFields().toStorage();
        for (String key : OTHER_FIELD_KEYS) {
            assertNotNull(fieldsBefore.get(key), "the real command should have written " + key);
        }
        JsonNode storedBefore = storedStudent(0);

        for (String command : List.of("level 1 l/Sec4", "level 1 l/JC2", "level 1 l/jc 2")) {
            logic.execute(command);

            Student after = first();
            assertEquals(before.getId(), after.getId(), command);
            assertSame(other, model.getStudentDirectory().getStudentList().getLast(), command);
            assertEquals(before.getName(), after.getName());
            assertEquals(before.getPhone(), after.getPhone());
            assertEquals(before.getAddress(), after.getAddress());
            assertEquals(before.getGuardianPhone(), after.getGuardianPhone());
            assertEquals(before.getHourlyRate(), after.getHourlyRate());
            assertEquals(WeeklySlotField.get(before), WeeklySlotField.get(after));
            for (String key : OTHER_FIELD_KEYS) {
                assertEquals(fieldsBefore.get(key), after.getStudentFields().toStorage().get(key), key);
                assertEquals(storedBefore.get(key), storedStudent(0).get(key), "stored " + key);
            }
        }
        assertEquals("JC 2", storedStudent(0).get("educationLevel").asText());
        assertEquals(before.getId().toString(), storedStudent(0).get("id").asText());
    }

    @Test
    public void otherCommandsAfterLevel_keepTheLevel() throws Exception {
        start();
        logic.execute("level 1 l/Sec4");

        setUpOtherFieldsWithRealCommands();
        assertEquals("Secondary 4", first().getStudentFields().display("educationLevel"));
        assertEquals("Secondary 4", storedStudent(0).get("educationLevel").asText());

        logic.execute("guardian 1 g/92345678");
        logic.execute("rate 1 r/80");
        logic.execute("subject 1 s/H2 Math");
        logic.execute("schedule 1 d/Wed t/16:30");
        logic.execute("edit 1 a/22 New Street");

        Student after = first();
        assertEquals("Secondary 4", after.getStudentFields().display("educationLevel"));
        assertEquals(new EducationLevel("Secondary 4"), after.getStudentFields().get(EducationLevel.FIELD)
                .orElseThrow());
        assertEquals("Secondary 4", storedStudent(0).get("educationLevel").asText());
    }

    @Test
    public void levelOnlyChangesTheTargetStudent() throws Exception {
        start();
        setUpOtherFieldsWithRealCommands();
        Student untouched = model.getStudentDirectory().getStudentList().getLast();

        logic.execute("level 1 l/P6");

        assertSame(untouched, model.getStudentDirectory().getStudentList().getLast());
        assertFalse(storedStudent(1).has("educationLevel"));
        assertTrue(storedStudent(0).has("educationLevel"));
    }

    @Test
    public void reloadedStudentEqualsTheInMemoryStudent() throws Exception {
        start();
        setUpOtherFieldsWithRealCommands();
        logic.execute("level 1 l/S3");

        Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();

        assertEquals(first(), reloaded);
        assertEquals(first().getId(), reloaded.getId());
        assertEquals("Secondary 3", reloaded.getStudentFields().display("educationLevel"));
        assertEquals("Tuesday 19:00", WeeklySlotField.display(reloaded));
    }
}
