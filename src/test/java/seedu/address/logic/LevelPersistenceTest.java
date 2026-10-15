package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Person;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** F05: the education level survives an application restart, and unset and legacy data load correctly. */
public class LevelPersistenceTest {
    private static final String LEGACY_RECORD = """
            {"persons":[{"name":"Legacy Lee","phone":"91234567","email":"legacy@example.com",
            "address":"1 Old Road","tags":["friends"],"guardianPhone":"+6591234567","hourlyRate":"40"}]}
            """;

    @TempDir
    private Path directory;

    private Path file;
    private final AtomicInteger writes = new AtomicInteger();

    /** One run of the application over the data file, as started from scratch. */
    private final class App {
        private final ModelManager model;
        private final Logic logic;

        private App(ModelManager model, JsonAddressBookStorage storage) {
            this.model = model;
            this.logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        }

        private Person first() {
            return model.getAddressBook().getPersonList().getFirst();
        }

        private String run(String command) throws Exception {
            return logic.execute(command).getFeedbackToUser();
        }
    }

    private JsonAddressBookStorage countingStorage() {
        return new JsonAddressBookStorage(file, new AtomicJsonFile() {
            @Override
            public void write(Path destination, String json) throws IOException {
                writes.incrementAndGet();
                super.write(destination, json);
            }
        });
    }

    private App restart() throws Exception {
        JsonAddressBookStorage storage = countingStorage();
        return new App(new ModelManager(storage.readAddressBook().orElseThrow(), new UserPrefs()), storage);
    }

    private void saveStudents(String... names) throws Exception {
        file = directory.resolve("students.json");
        ModelManager model = new ModelManager();
        for (int i = 0; i < names.length; i++) {
            model.addPerson(new PersonBuilder().withName(names[i]).withPhone("9123456" + i).build());
        }
        new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
    }

    private void writeRaw(String json) throws Exception {
        file = directory.resolve("students.json");
        Files.writeString(file, json);
    }

    private JsonNode stored(int position) throws Exception {
        return new ObjectMapper().readTree(Files.readString(file)).path("persons").get(position);
    }

    @Test
    public void restart_equivalentLevelIsNoChangeAndDifferentLevelReplacesIt() throws Exception {
        saveStudents("Alex Tan");
        assertEquals("Education level set for Alex Tan: Secondary 4.", restart().run("level 1 l/Secondary 4"));
        writes.set(0);

        App restarted = restart();
        assertEquals("Secondary 4", restarted.first().getStudentFields().display("educationLevel"));
        assertEquals("Education level for Alex Tan is already Secondary 4.", restarted.run("level 1 l/sec 4"));
        assertEquals(0, writes.get(), "an equivalent level must not write the file");
        assertNull(restarted.model.getSelectedPersonId());

        assertEquals("Education level updated for Alex Tan: Secondary 4 -> Primary 6.", restarted.run("level 1 l/P6"));
        assertEquals(1, writes.get());
        assertEquals("Primary 6", stored(0).get("educationLevel").asText());
        assertFalse(Files.readString(file).contains("Secondary 4"));
        assertEquals("Primary 6", restart().first().getStudentFields().display("educationLevel"));
    }

    @Test
    public void unsetLevel_isNotWrittenLoadsUnsetAndCanBeSetAfterRestart() throws Exception {
        saveStudents("Alex Tan", "Other Student");
        assertFalse(Files.readString(file).contains("educationLevel"));

        App restarted = restart();
        for (Person student : restarted.model.getAddressBook().getPersonList()) {
            assertTrue(student.getStudentFields().get(EducationLevel.FIELD).isEmpty());
            assertEquals("—", student.getStudentFields().display("educationLevel"));
        }

        assertEquals("Education level set for Other Student: JC 1.", restarted.run("level 2 l/JC1"));
        assertFalse(stored(0).has("educationLevel"));
        assertEquals("JC 1", stored(1).get("educationLevel").asText());
        assertEquals("—", restart().first().getStudentFields().display("educationLevel"));
    }

    @Test
    public void legacyRecord_loadsKeepsItsDataAcceptsALevelAndKeepsItAfterSaving() throws Exception {
        writeRaw(LEGACY_RECORD);

        App legacy = restart();
        Person loaded = legacy.first();
        assertTrue(loaded.getStudentFields().get(EducationLevel.FIELD).isEmpty());
        assertEquals("—", loaded.getStudentFields().display("educationLevel"));

        assertEquals("Education level set for Legacy Lee: Secondary 4.", legacy.run("level 1 l/Sec4"));

        Person updated = legacy.first();
        assertEquals(loaded.getId(), updated.getId());
        assertEquals("legacy@example.com", updated.getEmail().value);
        assertEquals(loaded.getTags(), updated.getTags());
        assertEquals("+6591234567", updated.getGuardianPhone().orElseThrow().value);
        assertEquals("40.00", updated.getHourlyRate().orElseThrow().toString());
        assertEquals(loaded.getId().toString(), stored(0).get("id").asText());
        assertEquals("Secondary 4", stored(0).get("educationLevel").asText());
        assertEquals("legacy@example.com", stored(0).get("email").asText());
        assertEquals("friends", stored(0).get("tags").get(0).asText());

        Person reloaded = restart().first();
        assertEquals(updated, reloaded);
        assertEquals(loaded.getId(), reloaded.getId());
    }

    @Test
    public void unreadableStoredLevel_isTreatedAsUnsetAndReplacedByTheCanonicalValue() throws Exception {
        writeRaw("""
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street",
                "educationLevel":"SECONDARY_1"}]}
                """);

        App app = restart();

        assertEquals("Education level set for Alex Tan: Secondary 1.", app.run("level 1 l/Sec1"));
        assertEquals("Secondary 1", stored(0).get("educationLevel").asText());
        assertFalse(Files.readString(file).contains("SECONDARY_1"));
        assertEquals("Education level for Alex Tan is already Secondary 1.", restart().run("level 1 l/s1"));
    }

    @Test
    public void unrelatedFutureFields_surviveALevelUpdate() throws Exception {
        writeRaw("""
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street",
                "futureField":"keep me","anotherFutureField":{"nested":[1,2,3]}}]}
                """);

        App app = restart();
        app.run("level 1 l/JC2");

        assertEquals("keep me", stored(0).get("futureField").asText());
        assertEquals(3, stored(0).get("anotherFutureField").get("nested").size());
        assertEquals("JC 2", stored(0).get("educationLevel").asText());
        assertEquals("JC 2", restart().first().getStudentFields().display("educationLevel"));
    }
}
