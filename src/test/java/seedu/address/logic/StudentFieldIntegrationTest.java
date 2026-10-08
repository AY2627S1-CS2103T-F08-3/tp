package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Regression tests for the F01/F02/F03/F04 merge contracts, using real commands and JSON persistence. */
public class StudentFieldIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void guardianRateAndEdit_preserveIdPositionAndUnrelatedFieldsAcrossReload() throws Exception {
        ModelManager model = model();
        Person original = model.getAddressBook().getPersonList().getFirst();
        Person other = model.getAddressBook().getPersonList().getLast();
        AtomicInteger writes = new AtomicInteger();
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            public void write(Path destination, String json) throws IOException {
                writes.incrementAndGet();
                super.write(destination, json);
            }
        };
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"), writer);
        Logic logic = logic(model, storage);
        assertEquals("Guardian contact updated for Alex Tan: +6591234567 -> +6592345678.",
                logic.execute("guardian 1 g/+65 9234 5678").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$40.00 -> S$80.00.",
                logic.execute("rate 1 r/80").getFeedbackToUser());
        logic.execute("edit 1 a/22 New Street");
        assertEquals(3, writes.get()); // exactly one atomic replacement per mutation
        Person updated = model.getAddressBook().getPersonList().getFirst();
        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getId(), model.getSelectedPersonId());
        assertSame(other, model.getAddressBook().getPersonList().getLast());
        assertUnrelatedFields(original, updated);
        assertEquals("+6592345678", updated.getGuardianPhone().orElseThrow().value);
        assertEquals("80.00", updated.getHourlyRate().orElseThrow().toString());
        assertEquals("+6592345678", updated.getStudentFields().display("guardianPhone"));
        assertEquals("80.00", updated.getStudentFields().display("hourlyRate"));
        Person reloaded = storage.readAddressBook().orElseThrow().getPersonList().getFirst();
        assertEquals(updated, reloaded);
        assertEquals(updated.getId(), reloaded.getId());
        String json = Files.readString(storage.getAddressBookFilePath());
        assertEquals(1, json.split("\"guardianPhone\"", -1).length - 1);
        assertEquals(1, json.split("\"hourlyRate\"", -1).length - 1);
    }

    @Test
    public void equivalentUpdates_preserveSelectionFilterObjectsFormattingAndDoNotSave() throws Exception {
        ModelManager model = model();
        Person original = model.getAddressBook().getPersonList().getFirst();
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
        assertEquals("Guardian contact for Alex Tan is already +6591234567.",
                logic.execute("guardian 1 g/+65-9123-4567").getFeedbackToUser());
        assertEquals("Hourly rate for Alex Tan is already S$40.00.",
                logic.execute("rate 1 r/40.0").getFeedbackToUser());
        logic.execute("edit 1 n/Alex   Tan");
        assertSame(original, model.getAddressBook().getPersonList().getFirst());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(selected.getId(), model.getSelectedPersonId());
        assertEquals("40.00", original.getStudentFields().display("hourlyRate"));
    }

    @Test
    public void failedFieldUpdatesAndDelete_preserveFileModelFilterAndSelection() throws Exception {
        for (String command : List.of("guardian 1 g/92345678", "rate 1 r/80", "edit 1 a/New Street", "delete 1")) {
            ModelManager model = model();
            Person target = model.getAddressBook().getPersonList().getFirst();
            model.updateFilteredPersonList(person -> person.getId().equals(target.getId()));
            model.selectPerson(target.getId());
            var predicate = model.getPersonPredicate();
            List<Person> before = List.copyOf(model.getAddressBook().getPersonList());
            Path file = directory.resolve("students.json");
            new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
            String saved = Files.readString(file);
            AtomicJsonFile writer = new AtomicJsonFile() {
                @Override
                protected void replace(Path temporary, Path destination) throws IOException {
                    throw new IOException("Injected atomic replacement failure");
                }
            };
            Logic logic = logic(model, new JsonAddressBookStorage(file, writer));
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());
            assertEquals(saved, Files.readString(file));
            assertEquals(before, model.getAddressBook().getPersonList());
            assertSame(target, model.getFilteredPersonList().getFirst());
            assertSame(predicate, model.getPersonPredicate());
            assertEquals(target.getId(), model.getSelectedPersonId());
        }
    }

    @Test
    public void fieldCommands_resolveFilteredIndicesAndHandleOversizedIndicesSafely() throws Exception {
        ModelManager model = model();
        Person target = model.getAddressBook().getPersonList().getLast();
        model.updateFilteredPersonList(person -> person.getId().equals(target.getId()));
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        logic.execute("guardian 1 g/61234567");
        assertEquals(target.getId(), model.getSelectedPersonId());
        assertEquals("+6561234567", model.getFilteredPersonList().getFirst().getGuardianPhone().orElseThrow().value);
        String huge = "9".repeat(100);
        for (String command : List.of("delete " + huge, "guardian " + huge + " g/81234567",
            "rate " + huge + " r/40", "edit " + huge + " n/Name")) {
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals("Error: No student exists at index " + huge + ".", failure.getMessage());
        }
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("guardianPhone", TextNode.valueOf("+6591234567"),
                "hourlyRate", TextNode.valueOf("40.00"), "educationLevel", TextNode.valueOf("SECONDARY_1"),
                "subject", TextNode.valueOf("Mathematics"), "weeklySlot", TextNode.valueOf("MONDAY 16:00"),
                "futureField", TextNode.valueOf("preserve me")));
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alex Tan").build().withStudentFields(fields));
        model.addPerson(new PersonBuilder().withName("Other Student").withPhone("91234567").build());
        return model;
    }

    private void assertUnrelatedFields(Person original, Person updated) {
        for (String key : List.of("educationLevel", "subject", "weeklySlot", "futureField")) {
            assertEquals(original.getStudentFields().toStorage().get(key),
                    updated.getStudentFields().toStorage().get(key));
        }
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getPhone(), updated.getPhone());
        assertEquals(original.getEmail(), updated.getEmail());
        assertEquals(original.getTags(), updated.getTags());
    }

    private Logic logic(ModelManager model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
