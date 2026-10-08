package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.model.person.Subject;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** End-to-end tests for the {@code subject} command, using real parsing, commands and JSON persistence. */
public class SubjectIntegrationTest {
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

        assertEquals("Subject set for Alex Tan: O-Level Chemistry.",
                logic.execute("subject 1 s/O-Level Chemistry").getFeedbackToUser());
        assertEquals("Subject updated for Alex Tan: O-Level Chemistry -> H2 Math.",
                logic.execute("subject 1 s/H2 Math").getFeedbackToUser());
        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/H2 Math").getFeedbackToUser());
        assertEquals(2, writes.get());
        assertEquals(target.getId(), model.getSelectedPersonId());
        assertEquals("H2 Math", displayedSubject(model));
    }

    @Test
    public void caseOnlyOrSpacingOnlyDifference_isNoChangeAndKeepsTheStoredDisplayValue() throws Exception {
        ModelManager model = model();
        Logic setup = logic(model, new JsonAddressBookStorage(directory.resolve("setup.json")));
        setup.execute("subject 1 s/H2 Math");
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

        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/h2 math").getFeedbackToUser());
        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/  H2     MATH  ").getFeedbackToUser());
        assertSame(first, model.getAddressBook().getPersonList().getFirst());
        assertEquals("H2 Math", displayedSubject(model));
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(selected.getId(), model.getSelectedPersonId());
    }

    @Test
    public void aChangeInContent_replacesTheSubjectAndShowsTheNewDisplayCase() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        logic.execute("subject 1 s/H2 Math");

        assertEquals("Subject updated for Alex Tan: H2 Math -> h2 maths.",
                logic.execute("subject 1 s/h2   maths").getFeedbackToUser());
        assertEquals("h2 maths", displayedSubject(model));
    }

    @Test
    public void slashesAndAllowedSymbolsInsideTheSubject_areAccepted() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        for (String subject : List.of("Math/Science", "Physics 2/3", "Arts & Crafts", "Children's Literature",
                "English (Oral) / Writing", "Primary 6 English")) {
            logic.execute("subject 1 s/" + subject);
            assertEquals(subject, displayedSubject(model));
        }
    }

    @Test
    public void update_keepsIdPositionUnrelatedFieldsAndPersistsOnceAcrossReload() throws Exception {
        ModelManager model = model();
        Person original = model.getAddressBook().getPersonList().getFirst();
        Person other = model.getAddressBook().getPersonList().getLast();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        Logic logic = logic(model, storage);

        logic.execute("subject 1 s/H2 Math");
        logic.execute("subject 1 s/O-Level Chemistry");

        Person updated = model.getAddressBook().getPersonList().getFirst();
        assertEquals(original.getId(), updated.getId());
        assertSame(other, model.getAddressBook().getPersonList().getLast());
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getPhone(), updated.getPhone());
        assertEquals(original.getAddress(), updated.getAddress());
        assertEquals(original.getGuardianPhone(), updated.getGuardianPhone());
        assertEquals(original.getHourlyRate(), updated.getHourlyRate());
        for (String key : List.of("educationLevel", "weeklySlot", "futureField")) {
            assertEquals(original.getStudentFields().toStorage().get(key),
                    updated.getStudentFields().toStorage().get(key), key);
        }
        assertEquals(new Subject("O-Level Chemistry"), updated.getStudentFields().get(Subject.FIELD).orElseThrow());

        String json = Files.readString(storage.getAddressBookFilePath());
        assertEquals(1, json.split("\"subject\"", -1).length - 1, "replacement must not accumulate");
        assertTrue(json.contains("\"O-Level Chemistry\""));
        assertFalse(json.contains("H2 Math"));
        Person reloaded = storage.readAddressBook().orElseThrow().getPersonList().getFirst();
        assertEquals(updated, reloaded);
        assertEquals("O-Level Chemistry", reloaded.getStudentFields().display("subject"));
    }

    @Test
    public void laterGuardianRateAndEditCommands_keepTheSubject() throws Exception {
        ModelManager model = model();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        logic.execute("subject 1 s/H2 Math");
        logic.execute("guardian 1 g/92345678");
        logic.execute("rate 1 r/80");
        logic.execute("edit 1 a/22 New Street");
        assertEquals("H2 Math", displayedSubject(model));
    }

    @Test
    public void successfulUpdate_selectsTheTargetStudent() throws Exception {
        ModelManager model = model();
        Person last = model.getAddressBook().getPersonList().getLast();
        model.selectPerson(last.getId());
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        logic.execute("subject 1 s/Math");
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

        CommandException failure = assertThrows(CommandException.class, () -> logic.execute("subject 1 s/Math"));

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

        assertParseError(logic, "subject 1 s/Math@Home", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/A", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/12", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/" + "a".repeat(51), Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1 s/", Subject.MESSAGE_CONSTRAINTS);
        assertParseError(logic, "subject 1", "Error: Missing required parameter: s/SUBJECT.");
        assertParseError(logic, "subject 1 s/Math s/Physics", "Error: Parameter s/ may be specified only once.");
        assertParseError(logic, "subject 1 l/Sec4", "Error: Unknown parameter: l/.");
        assertParseError(logic, "subject 1 foo s/Math", "Error: Unexpected text after command.");
        assertParseError(logic, "subject 0 s/Math", INVALID_INDEX);
        assertParseError(logic, "subject s/Math", INVALID_INDEX);
        assertParseError(logic, "subject abc s/@", INVALID_INDEX);
        // Values are validated before the row is looked up, so a bad value wins over a missing student.
        assertParseError(logic, "subject 99 s/@", Subject.MESSAGE_CONSTRAINTS);
        assertCommandError(logic, "subject 99 s/Math", "Error: No student exists at index 99.");
        String huge = "9".repeat(100);
        assertCommandError(logic, "subject " + huge + " s/Math", "Error: No student exists at index " + huge + ".");

        assertEquals(before, model.getAddressBook().getPersonList());
        assertEquals(selected.getId(), model.getSelectedPersonId());
    }

    @Test
    public void subject_resolvesTheDisplayedListAndManyStudentsMayShareASubject() throws Exception {
        ModelManager model = model();
        Person second = model.getAddressBook().getPersonList().getLast();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        model.updateFilteredPersonList(person -> person.getId().equals(second.getId()));

        logic.execute("subject 1 s/H2 Math");
        model.updateFilteredPersonList(person -> true);
        logic.execute("subject 1 s/h2 math");

        for (Person student : model.getAddressBook().getPersonList()) {
            assertEquals(new Subject("H2 Math"), student.getStudentFields().get(Subject.FIELD).orElseThrow());
        }
    }

    @Test
    public void deletingAStudent_removesTheirSubjectFromStorage() throws Exception {
        ModelManager model = model();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        Logic logic = logic(model, storage);
        logic.execute("subject 1 s/H2 Math");
        assertTrue(Files.readString(storage.getAddressBookFilePath()).contains("\"subject\""));

        logic.execute("delete 1");

        assertFalse(Files.readString(storage.getAddressBookFilePath()).contains("\"subject\""));
    }

    @Test
    public void recordsWithoutASubject_loadAsUnsetAndShowAnEmDash() throws Exception {
        ModelManager model = model();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        storage.saveAddressBook(model.getAddressBook());

        Person loaded = storage.readAddressBook().orElseThrow().getPersonList().getLast();

        assertTrue(loaded.getStudentFields().get(Subject.FIELD).isEmpty());
        assertEquals("—", loaded.getStudentFields().display("subject"));
    }

    private static String displayedSubject(ModelManager model) {
        return model.getAddressBook().getPersonList().getFirst().getStudentFields().display("subject");
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
                "hourlyRate", TextNode.valueOf("40.00"), "educationLevel", TextNode.valueOf("Secondary 4"),
                "futureField", TextNode.valueOf("preserve me")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
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
