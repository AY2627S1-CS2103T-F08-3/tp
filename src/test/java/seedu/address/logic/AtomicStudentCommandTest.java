package seedu.address.logic;

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

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class AtomicStudentCommandTest {
    @TempDir
    private Path directory;

    @Test
    public void add_savesCompleteStateBeforePublishingAndSurvivesReload() throws Exception {
        ModelManager model = existingModel();
        Person first = model.getAddressBook().getPersonList().getFirst();
        AtomicInteger saves = new AtomicInteger();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook proposed) throws IOException {
                assertEquals(List.of(first), model.getAddressBook().getPersonList());
                assertEquals(first.getId(), model.getSelectedPersonId());
                assertEquals(2, proposed.getPersonList().size());
                assertEquals("21 Clementi Ave 3 #04-18", proposed.getPersonList().getLast().getAddress().value);
                super.saveAddressBook(proposed);
                saves.incrementAndGet();
            }
        };
        Logic logic = logic(model, storage);
        assertEquals("Student added: Alex Tan.", logic.execute(
                "  add   n/Ａlex　 Tan p/+65-8123-4567 a/21 Clementi Ave 3 #04-18  ").getFeedbackToUser());
        Person added = model.getAddressBook().getPersonList().getLast();
        assertEquals(1, saves.get());
        assertEquals("+6581234567", added.getPhone().value);
        assertTrue(added.getStudentFields().toStorage().isEmpty());
        assertTrue(added.getEmail().value.isEmpty());
        assertTrue(added.getTags().isEmpty());
        assertEquals(added.getId(), model.getSelectedPersonId());
        assertEquals(2, model.getFilteredPersonList().size());
        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        assertEquals(first.getId(), reloaded.getPersonList().getFirst().getId());
        assertEquals(added.getId(), reloaded.getPersonList().getLast().getId());
    }

    @Test
    public void add_failedReplacementPreservesFileModelFilterAndSelection() throws Exception {
        ModelManager model = existingModel();
        Person first = model.getAddressBook().getPersonList().getFirst();
        Path file = directory.resolve("students.json");
        new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
        byte[] before = Files.readAllBytes(file);
        AtomicJsonFile failingWriter = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path target) throws IOException {
                throw new IOException("Injected atomic replacement failure");
            }
        };
        Logic logic = logic(model, new JsonAddressBookStorage(file, failingWriter));
        var predicate = model.getPersonPredicate();
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("add n/Alex Tan p/81234567 a/21 Clementi Ave"));
        assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, error.getMessage());
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(List.of(first), model.getAddressBook().getPersonList());
        assertEquals(List.of(first), model.getFilteredPersonList());
        assertEquals(predicate, model.getPersonPredicate());
        assertEquals(first.getId(), model.getSelectedPersonId());
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
        Logic logic = logic(model, new JsonAddressBookStorage(file, failingWriter));
        assertThrows(CommandException.class, () -> logic.execute("add n/Alex p/81234567 a/A"));
        assertFalse(Files.exists(file));
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertNull(model.getSelectedPersonId());
    }

    @Test
    public void readOnlyAndEquivalentEdit_doNotSave() throws Exception {
        ModelManager model = existingModel();
        Person first = model.getAddressBook().getPersonList().getFirst();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook proposed) {
                throw new AssertionError("Unchanged data must not be saved");
            }
        };
        Logic logic = logic(model, storage);
        logic.execute("help");
        assertEquals(first.getId(), model.getSelectedPersonId());
        logic.execute("edit 1 n/Existing Student");
        assertEquals(first.getId(), model.getAddressBook().getPersonList().getFirst().getId());
    }

    private ModelManager existingModel() {
        ModelManager model = new ModelManager();
        Person first = new Person(new Name("Existing Student"), new Phone("91234567"), new Address("8 Jalan Besar"));
        model.addPerson(first);
        model.updateFilteredPersonList(student -> student.getId().equals(first.getId()));
        model.selectPerson(first.getId());
        return model;
    }

    private Logic logic(ModelManager model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
