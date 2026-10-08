package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.fasterxml.jackson.databind.node.TextNode;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** End-to-end tests for the {@code level} command, using real parsing, commands and JSON persistence. */
public class LevelIntegrationTest {
    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";

    @TempDir
    private Path directory;

    @Test
    public void setReplaceAndNoChange_useExactMessagesAndSaveOnlyRealChanges() throws Exception {
        ModelManager model = model();
        Person target = model.getAddressBook().getPersonList().getFirst();
        AtomicInteger writes = new AtomicInteger();
        Logic logic = logic(model, countingStorage(writes));

        assertEquals("Education level set for Alex Tan: Secondary 4.",
                logic.execute("level 1 l/Secondary 4").getFeedbackToUser());
        assertEquals("Education level updated for Alex Tan: Secondary 4 -> JC 2.",
                logic.execute("level 1 l/jc2").getFeedbackToUser());
        assertEquals("Education level for Alex Tan is already JC 2.",
                logic.execute("level 1 l/JC 2").getFeedbackToUser());
        assertEquals("Education level for Alex Tan is already JC 2.",
                logic.execute("level 1 l/jc2").getFeedbackToUser());
        assertEquals(2, writes.get());
        assertEquals(target.getId(), model.getSelectedPersonId());
        assertEquals("JC 2", model.getAddressBook().getPersonList().getFirst().getStudentFields()
                .display("educationLevel"));
    }

    @Test
    public void everyAcceptedAlias_isStoredAsCanonicalName() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        for (int grade = 1; grade <= 6; grade++) {
            logic.execute("level 1 l/P" + grade);
            assertEquals("Primary " + grade, displayedLevel(model));
        }
        for (int grade = 1; grade <= 5; grade++) {
            logic.execute("level 1 l/Sec" + grade);
            assertEquals("Secondary " + grade, displayedLevel(model));
            logic.execute("level 1 l/s " + grade);
            assertEquals("Secondary " + grade, displayedLevel(model));
        }
        for (int grade = 1; grade <= 2; grade++) {
            logic.execute("level 1 l/JC" + grade);
            assertEquals("JC " + grade, displayedLevel(model));
        }
    }

    @Test
    public void update_keepsIdPositionUnrelatedFieldsAndPersistsOnceAcrossReload() throws Exception {
        ModelManager model = model();
        Person original = model.getAddressBook().getPersonList().getFirst();
        Person other = model.getAddressBook().getPersonList().getLast();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        Logic logic = logic(model, storage);

        logic.execute("level 1 l/Sec4");
        logic.execute("level 1 l/P6");

        Person updated = model.getAddressBook().getPersonList().getFirst();
        assertEquals(original.getId(), updated.getId());
        assertSame(other, model.getAddressBook().getPersonList().getLast());
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getPhone(), updated.getPhone());
        assertEquals(original.getAddress(), updated.getAddress());
        assertEquals(original.getGuardianPhone(), updated.getGuardianPhone());
        assertEquals(original.getHourlyRate(), updated.getHourlyRate());
        for (String key : List.of("subject", "weeklySlot", "futureField")) {
            assertEquals(original.getStudentFields().toStorage().get(key),
                    updated.getStudentFields().toStorage().get(key), key);
        }
        assertEquals(new EducationLevel("Primary 6"), updated.getStudentFields().get(EducationLevel.FIELD)
                .orElseThrow());

        String json = Files.readString(storage.getAddressBookFilePath());
        assertEquals(1, json.split("\"educationLevel\"", -1).length - 1, "replacement must not accumulate");
        assertTrue(json.contains("\"Primary 6\""));
        assertFalse(json.contains("Secondary 4"));
        Person reloaded = storage.readAddressBook().orElseThrow().getPersonList().getFirst();
        assertEquals(updated, reloaded);
        assertEquals(updated.getId(), reloaded.getId());
    }

    @Test
    public void laterGuardianRateAndEditCommands_keepTheLevel() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        logic.execute("level 1 l/Sec4");
        logic.execute("guardian 1 g/92345678");
        logic.execute("rate 1 r/80");
        logic.execute("edit 1 a/22 New Street");
        assertEquals("Secondary 4", displayedLevel(model));
    }

    @Test
    public void equivalentInput_preservesSelectionFilterObjectsAndDoesNotSave() throws Exception {
        ModelManager model = model();
        Logic setup = logic(model, new JsonAddressBookStorage(directory.resolve("setup.json")));
        setup.execute("level 1 l/Secondary 4");
        Person first = model.getAddressBook().getPersonList().getFirst();
        Person selected = model.getAddressBook().getPersonList().getLast();
        model.updateFilteredPersonList(person -> true);
        model.selectPerson(selected.getId());
        var predicate = model.getPersonPredicate();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook state) {
                throw new AssertionError("No-change commands must not save");
            }
        };
        Logic logic = logic(model, storage);

        assertEquals("Education level for Alex Tan is already Secondary 4.",
                logic.execute("level 1 l/sec 4").getFeedbackToUser());
        assertSame(first, model.getAddressBook().getPersonList().getFirst());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(selected.getId(), model.getSelectedPersonId());
    }

    @Test
    public void successfulUpdate_selectsTheTargetStudent() throws Exception {
        ModelManager model = model();
        Person last = model.getAddressBook().getPersonList().getLast();
        model.selectPerson(last.getId());
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        logic.execute("level 1 l/P4");
        assertEquals(model.getAddressBook().getPersonList().getFirst().getId(), model.getSelectedPersonId());
    }

    @Test
    public void saveFailure_preservesFileModelFilterAndSelection() throws Exception {
        ModelManager model = model();
        Person target = model.getAddressBook().getPersonList().getFirst();
        model.updateFilteredPersonList(person -> person.getId().equals(target.getId()));
        model.selectPerson(target.getId());
        var predicate = model.getPersonPredicate();
        List<Person> before = List.copyOf(model.getAddressBook().getPersonList());
        Path file = directory.resolve("students.json");
        new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
        String saved = Files.readString(file);
        AtomicJsonFile failingWriter = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path destination) throws IOException {
                throw new IOException("Injected atomic replacement failure");
            }
        };
        Logic logic = logic(model, new JsonAddressBookStorage(file, failingWriter));

        CommandException failure = assertThrows(CommandException.class, () -> logic.execute("level 1 l/Sec4"));

        assertEquals("Error: Changes could not be saved. No changes were made.", failure.getMessage());
        assertEquals(saved, Files.readString(file));
        assertEquals(before, model.getAddressBook().getPersonList());
        assertSame(target, model.getFilteredPersonList().getFirst());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(target.getId(), model.getSelectedPersonId());
    }

    @Test
    public void errors_useExactMessagesAndLeaveStateUntouched() throws Exception {
        ModelManager model = model();
        Person selected = model.getAddressBook().getPersonList().getLast();
        model.selectPerson(selected.getId());
        List<Person> before = List.copyOf(model.getAddressBook().getPersonList());
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));

        assertParseError(logic, "level 1 l/Sec 6", EducationLevel.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "level 1 l/", EducationLevel.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "level 1", "Error: Missing required parameter: l/LEVEL.");
        assertParseError(logic, "level 1 l/Sec4 l/Sec5", "Error: Parameter l/ may be specified only once.");
        assertParseError(logic, "level 1 x/foo", "Error: Unknown parameter: x/.");
        assertParseError(logic, "level 1 foo l/Sec4", "Error: Unexpected text after command.");
        assertParseError(logic, "level 0 l/Sec4", INVALID_INDEX);
        assertParseError(logic, "level l/Sec4", INVALID_INDEX);
        assertParseError(logic, "level abc l/Bad", INVALID_INDEX);
        // Values are validated before the row is looked up, so a bad value wins over a missing student.
        assertParseError(logic, "level 99 l/Bad", EducationLevel.MESSAGE_CONSTRAINTS);
        assertCommandError(logic, "level 99 l/P1", "Error: No student exists at index 99.");
        String huge = "9".repeat(100);
        assertCommandError(logic, "level " + huge + " l/P1", "Error: No student exists at index " + huge + ".");

        assertEquals(before, model.getAddressBook().getPersonList());
        assertEquals(selected.getId(), model.getSelectedPersonId());
    }

    @Test
    public void level_resolvesTheDisplayedListAndManyStudentsMayShareALevel() throws Exception {
        ModelManager model = model();
        Person second = model.getAddressBook().getPersonList().getLast();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        model.updateFilteredPersonList(person -> person.getId().equals(second.getId()));

        logic.execute("level 1 l/Sec4");
        model.updateFilteredPersonList(person -> true);
        logic.execute("level 1 l/Sec4");

        for (Person student : model.getAddressBook().getPersonList()) {
            assertEquals("Secondary 4", student.getStudentFields().display("educationLevel"));
        }
    }

    @Test
    public void deletingAStudent_removesTheirLevelFromStorage() throws Exception {
        ModelManager model = model();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        Logic logic = logic(model, storage);
        logic.execute("level 1 l/Sec4");
        assertTrue(Files.readString(storage.getAddressBookFilePath()).contains("educationLevel"));

        logic.execute("delete 1");

        assertFalse(Files.readString(storage.getAddressBookFilePath()).contains("educationLevel"));
    }

    @Test
    public void recordsWithoutALevel_loadAsUnsetAndShowAnEmDash() throws Exception {
        ModelManager model = model();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        storage.saveAddressBook(model.getAddressBook());

        Person loaded = storage.readAddressBook().orElseThrow().getPersonList().getLast();

        assertTrue(loaded.getStudentFields().get(EducationLevel.FIELD).isEmpty());
        assertEquals("—", loaded.getStudentFields().display("educationLevel"));
    }

    private static String displayedLevel(ModelManager model) {
        return model.getAddressBook().getPersonList().getFirst().getStudentFields().display("educationLevel");
    }

    private static void assertParseError(Logic logic, String command, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> logic.execute(command)).getMessage(), command);
    }

    private static void assertCommandError(Logic logic, String command, String message) {
        assertEquals(message, assertThrows(CommandException.class, () -> logic.execute(command)).getMessage(),
                command);
    }

    private JsonAddressBookStorage countingStorage(AtomicInteger writes) {
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            public void write(Path destination, String json) throws IOException {
                writes.incrementAndGet();
                super.write(destination, json);
            }
        };
        return new JsonAddressBookStorage(directory.resolve("students.json"), writer);
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("guardianPhone", TextNode.valueOf("+6591234567"),
                "hourlyRate", TextNode.valueOf("40.00"), "subject", TextNode.valueOf("Mathematics"),
                "weeklySlot", TextNode.valueOf("MONDAY 16:00"), "futureField", TextNode.valueOf("preserve me")));
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alex Tan").build().withStudentFields(fields));
        model.addPerson(new PersonBuilder().withName("Other Student").withPhone("91234567").build());
        return model;
    }

    private Logic logic(ModelManager model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
