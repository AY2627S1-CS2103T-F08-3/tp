package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult.SelectionAction;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

class ListStudentIntegrationTest {
    @TempDir
    Path directory;

    private Logic logic(Model model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    @Test
    void list_loadsStoredOrderClearsSelectionAndDoesNotSave() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.selectPerson(model.getFilteredPersonList().get(0).getId());
        Path path = directory.resolve("register.json");
        new JsonAddressBookStorage(path).saveAddressBook(getTypicalAddressBook());
        JsonAddressBookStorage storage = new JsonAddressBookStorage(path) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook proposed) {
                throw new AssertionError("list must never save");
            }
        };
        var result = logic(model, storage).execute("list");
        assertEquals("Listed 7 students.", result.getFeedbackToUser());
        assertEquals(SelectionAction.CLEAR, result.getSelectionAction());
        assertNull(model.getSelectedPersonId());
        assertEquals(getTypicalAddressBook().getPersonList(), model.getFilteredPersonList());
    }

    @Test
    void list_emptyRegister_returnsEmptyMessage() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("register.json"));
        storage.saveAddressBook(new AddressBook());
        assertEquals(ListCommand.MESSAGE_EMPTY, logic(model, storage).execute("list").getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    void list_corruptStorageAndInvalidInput_preserveModelAndFilteredView() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        AddressBook before = new AddressBook(model.getAddressBook());
        Person visible = model.getFilteredPersonList().get(2);
        model.updateFilteredPersonList(p -> p == visible);
        model.selectPerson(visible.getId());
        Path path = directory.resolve("register.json");
        Files.writeString(path, "invalid json");
        Logic logic = logic(model, new JsonAddressBookStorage(path));
        assertThrows(ParseException.class, "Error: List does not accept parameters.", () -> logic.execute("list 1"));
        assertThrows(CommandException.class, ListCommand.MESSAGE_LOAD_FAILURE, () -> logic.execute("list"));
        assertEquals(before, model.getAddressBook());
        assertEquals(List.of(visible), model.getFilteredPersonList());
        assertEquals(visible.getId(), model.getSelectedPersonId());
        assertEquals("invalid json", Files.readString(path));
    }

}
