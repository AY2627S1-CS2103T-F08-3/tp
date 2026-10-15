package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Regression checks for human-edited JSON and all-or-nothing reload. */
public class DataFileSafetyTest {
    private static final String RECORD =
            "{\"name\":\"Alex Tan\",\"phone\":\"81234567\",\"address\":\"21 Street\"}";

    @TempDir
    private Path directory;

    @Test
    public void malformedRecords_reportLoadFailureWithoutPublishingOrOverwriting() throws Exception {
        Path file = directory.resolve("students.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        ModelManager model = new ModelManager();
        Logic logic = logic(model, storage);
        logic.execute("add n/Alex Tan p/81234567 a/21 Street");
        Person selected = model.getFilteredPersonList().getFirst();
        model.updateFilteredPersonList(student -> student.getId().equals(selected.getId()));
        model.selectPerson(selected.getId());
        var predicate = model.getPersonPredicate();
        List<String> invalidFiles = List.of("null", "{}", "{\"persons\":null}", "{\"persons\":[null]}",
                "{\"persons\":[" + RECORD + ",null]}",
                "{\"persons\":[" + RECORD.replace("}", ",\"tags\":[null]}") + "]}");
        for (String json : invalidFiles) {
            Files.writeString(file, json);
            byte[] before = Files.readAllBytes(file);
            assertThrows(DataLoadingException.class, storage::readAddressBook, json);
            CommandException error = assertThrows(CommandException.class, () -> logic.execute("list"), json);
            assertEquals(ListCommand.MESSAGE_LOAD_FAILURE, error.getMessage());
            assertEquals(List.of(selected), model.getAddressBook().getPersonList());
            assertEquals(List.of(selected), model.getFilteredPersonList());
            assertSame(selected, model.getFilteredPersonList().getFirst());
            assertSame(predicate, model.getPersonPredicate());
            assertEquals(selected.getId(), model.getSelectedPersonId());
            assertArrayEquals(before, Files.readAllBytes(file));
        }
    }

    @Test
    public void validManualEdit_loadsWithoutLosingIdentityOrUnrelatedFields() throws Exception {
        Path file = directory.resolve("students.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        ModelManager model = new ModelManager();
        Logic logic = logic(model, storage);
        logic.execute("add n/Alex Tan p/81234567 a/21 Street");
        logic.execute("guardian 1 g/91234567");
        var id = model.getFilteredPersonList().getFirst().getId();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = (ObjectNode) mapper.readTree(Files.readString(file));
        ObjectNode record = (ObjectNode) json.get("persons").get(0);
        record.put("address", "  12/3   New Street #04-18  ");
        record.putObject("futureOwnerField").put("note", "keep me");
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
        byte[] edited = Files.readAllBytes(file);
        logic.execute("list");
        Person loaded = model.getFilteredPersonList().getFirst();
        assertEquals("12/3 New Street #04-18", loaded.getAddress().value);
        assertEquals(id, loaded.getId());
        assertEquals("+6591234567", loaded.getGuardianPhone().orElseThrow().value);
        assertTrue(loaded.getStudentFields().toStorage().containsKey("futureOwnerField"));
        assertArrayEquals(edited, Files.readAllBytes(file));
        storage.saveAddressBook(model.getAddressBook());
        assertEquals(loaded, storage.readAddressBook().orElseThrow().getPersonList().getFirst());
    }

    private Logic logic(ModelManager model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("preferences.json"))));
    }
}
