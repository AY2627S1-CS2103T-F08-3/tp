package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class AddStudentEndToEndTest {
    @TempDir
    private Path directory;

    @Test
    public void invalidCommands_preservePublishedStateAndSavedBytes() throws Exception {
        Path file = directory.resolve("students.json");
        ModelManager model = new ModelManager();
        Logic logic = logic(model, file);
        logic.execute("add n/Alex Tan p/81234567 a/21 Clementi Ave 3 #04-18");
        Person student = model.getAddressBook().getPersonList().getFirst();
        model.updateFilteredPersonList(person -> person.getId().equals(student.getId()));
        model.selectPerson(student.getId());
        var predicate = model.getPersonPredicate();
        byte[] before = Files.readAllBytes(file);
        String[][] cases = {
            {"ADD x/bad", "Error: Unknown command. Type help to view available commands."},
            {"add text n/Alex p/81234567 a/A", "Error: Unexpected text after command."},
            {"add n/123 a/--- x/bad", "Error: Unknown parameter: x/."},
            {"add n/123 a/---", "Error: Missing required parameter: p/PHONE."},
            {"add n/123 n/Alex p/bad a/---", "Error: Parameter n/ may be specified only once."},
            {"add a/--- p/bad n/", "Error: Invalid name. Use 1-70 letters with spaces, apostrophes, "
                    + "hyphens, or full stops."},
            {"add n/Alex p/ a/---", "Error: Invalid phone. Use an 8-digit Singapore number beginning with "
                    + "6, 8, or 9, optionally prefixed by +65."},
            {"add n/Alex p/81234567 a/A\n", "Error: Invalid address. Use 1-200 letters, numbers, spaces, "
                    + "or common address punctuation."},
            {"add n/ＡLEX   TAN p/+65-8123-4567 a/Different address",
                "Error: This student already exists with the same name and phone."}
        };
        for (String[] testCase : cases) {
            Exception error = assertThrows(Exception.class, () -> logic.execute(testCase[0]));
            assertEquals(testCase[1], error.getMessage(), testCase[0]);
            assertEquals(List.of(student), model.getAddressBook().getPersonList());
            assertEquals(List.of(student), model.getFilteredPersonList());
            assertEquals(student.getId(), model.getSelectedPersonId());
            assertEquals(predicate, model.getPersonPredicate());
            assertArrayEquals(before, Files.readAllBytes(file));
        }
    }

    @Test
    public void bothUserExamples_sharedPhoneAndSameName_surviveReloadInOrder() throws Exception {
        Path file = directory.resolve("students.json");
        ModelManager model = new ModelManager();
        Logic logic = logic(model, file);
        logic.execute("add n/Alex Tan p/81234567 a/21 Clementi Ave 3 #04-18");
        assertEquals("Student added: Nur Aisyah.", logic.execute(
                "add a/8 Jalan Besar p/+65 9234 5678 n/Nur Aisyah").getFeedbackToUser());
        assertEquals("Student added: Alex Tan.", logic.execute(
                "add n/Alex Tan p/61234567 a/12/3 Street").getFeedbackToUser());
        assertEquals("Student added: Mei Tan. Warning: Another student uses this phone number.", logic.execute(
                "add n/Mei Tan p/+65 8123 4567 a/21 Clementi Ave 3 #04-18").getFeedbackToUser());
        List<Person> students = List.copyOf(model.getAddressBook().getPersonList());
        assertEquals(students.getLast().getId(), model.getSelectedPersonId());
        ModelManager reloaded = new ModelManager(new JsonAddressBookStorage(file).readAddressBook().orElseThrow(),
                new UserPrefs());
        assertEquals(students, reloaded.getAddressBook().getPersonList());
        assertEquals(students.stream().map(Person::getId).toList(),
                reloaded.getAddressBook().getPersonList().stream().map(Person::getId).toList());
    }

    private Logic logic(ModelManager model, Path file) {
        return new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
