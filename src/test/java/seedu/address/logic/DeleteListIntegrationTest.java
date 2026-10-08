package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.TextNode;

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
import seedu.address.model.person.StudentFields;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Exercises commands through parsing, persistence, and the published model. */
class DeleteListIntegrationTest {
    @TempDir
    Path directory;

    private Logic logic(Model model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    @Test
    void delete_firstMiddleLast_persistCompleteRemainingProfiles() throws Exception {
        for (int index : List.of(1, 4, 7)) {
            Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
            List<Person> expected = new ArrayList<>(model.getFilteredPersonList());
            Person removed = expected.remove(index - 1);
            JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("register.json"));
            var result = logic(model, storage).execute("delete " + index);
            assertEquals("Student deleted: " + removed.getName() + ".", result.getFeedbackToUser());
            assertEquals(SelectionAction.PRESERVE, result.getSelectionAction());
            assertEquals(expected, model.getFilteredPersonList());
            assertEquals(expected, storage.readAddressBook().orElseThrow().getPersonList());
            assertFalse(storage.readAddressBook().orElseThrow().getPersonList().contains(removed));
        }
    }

    @Test
    void delete_filteredList_targetsVisibleRowAndRetainsSurvivingObjects() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Person first = model.getFilteredPersonList().get(0);
        Person target = model.getFilteredPersonList().get(3);
        Person survivor = model.getFilteredPersonList().get(5);
        model.updateFilteredPersonList(p -> p == target || p == survivor);
        logic(model, new JsonAddressBookStorage(directory.resolve("register.json"))).execute("delete 1");
        assertEquals(List.of(survivor), model.getFilteredPersonList());
        assertSame(survivor, model.getFilteredPersonList().get(0));
        assertSame(first, model.getAddressBook().getPersonList().get(0));
    }

    @Test
    void delete_saveFailure_preservesStorageModelAndView() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Path path = directory.resolve("register.json");
        new JsonAddressBookStorage(path).saveAddressBook(model.getAddressBook());
        String original = Files.readString(path);
        AddressBook before = new AddressBook(model.getAddressBook());
        Person visible = model.getFilteredPersonList().get(3);
        model.updateFilteredPersonList(p -> p == visible);
        model.selectPerson(visible.getId());
        JsonAddressBookStorage failing = new JsonAddressBookStorage(path) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook proposed) throws IOException {
                assertEquals(before, model.getAddressBook());
                assertEquals(6, proposed.getPersonList().size());
                throw new IOException("save failed");
            }
        };
        assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_FAILURE, () ->
                logic(model, failing).execute("delete 1"));
        assertEquals(before, model.getAddressBook());
        assertEquals(List.of(visible), model.getFilteredPersonList());
        assertEquals(visible.getId(), model.getSelectedPersonId());
        assertEquals(original, Files.readString(path));
    }

    @Test
    void list_loadsStoredOrderClearsSelectionAndDoesNotSave() throws Exception {
        Model model = new ModelManager();
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

    @Test
    void delete_profileWithAttachedData_removedAfterReload() throws Exception {
        Model model = new ModelManager();
        Person target = new PersonBuilder().withName("Student Target")
                .withTags("math", "weekly").build();
        model.addPerson(target);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("register.json"));
        storage.saveAddressBook(model.getAddressBook());
        assertEquals(target, storage.readAddressBook().orElseThrow().getPersonList().get(0));
        logic(model, storage).execute("delete 1");
        assertEquals(List.of(), storage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    void delete_studentEnvelope_removedFromProposedStateWithoutChangingSurvivor() throws Exception {
        Model model = new ModelManager();
        StudentFields fields = new StudentFields(Map.of("subject", TextNode.valueOf("Math"),
                "guardianPhone", TextNode.valueOf("+6591234567")));
        Person target = new PersonBuilder().withName("Target").build().withStudentFields(fields);
        Person survivor = new PersonBuilder().withName("Survivor").build().withStudentFields(fields);
        model.addPerson(target);
        model.addPerson(survivor);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("register.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook proposed) {
                assertEquals(List.of(survivor), proposed.getPersonList());
                assertEquals(fields, proposed.getPersonList().get(0).getStudentFields());
            }
        };
        logic(model, storage).execute("delete 1");
        assertEquals(List.of(survivor), model.getFilteredPersonList());
    }


    @Test
    void delete_duplicateNames_preservesOtherProfileAndSelectionById() throws Exception {
        Model model = new ModelManager();
        StudentFields fields = new StudentFields(Map.of("subject", TextNode.valueOf("Math"),
                "guardianPhone", TextNode.valueOf("+6591234567"),
                "educationLevel", TextNode.valueOf("Primary 1"),
                "hourlyRate", TextNode.valueOf("40.00")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        Person target = new PersonBuilder().withName("Same Name").withPhone("81234567")
                .build().withStudentFields(fields);
        Person survivor = new PersonBuilder().withName("Same Name").withPhone("91234567")
                .build().withStudentFields(fields);
        model.addPerson(target);
        model.addPerson(survivor);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("register.json"));
        Logic logic = logic(model, storage);
        logic.execute("delete 1");
        assertEquals(survivor.getId(), model.getSelectedPersonId());
        Person reloaded = storage.readAddressBook().orElseThrow().getPersonList().get(0);
        assertEquals(survivor.getId(), reloaded.getId());
        assertEquals(fields, reloaded.getStudentFields());
        logic.execute("delete 1");
        assertNull(model.getSelectedPersonId());
        assertEquals(List.of(), storage.readAddressBook().orElseThrow().getPersonList());
    }

}
