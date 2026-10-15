package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.model.person.Subject;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Remaining F06 acceptance cases across fresh models and real atomic file replacement. */
public class SubjectLifecycleTest {
    @TempDir
    private Path directory;

    @Test
    public void restart_noChangeAndReplacement_preserveCompleteProfileAndOtherStudents() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alex Tan").withTags("legacy").build()
                .withStudentFields(new StudentFields(Map.of("futureField",
                        new ObjectMapper().readTree("{\"nested\":[1,\"keep\"]}")))));
        model.addPerson(new PersonBuilder().withName("Other Student").build());
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        Logic setup = logic(model, storage);
        for (String command : List.of("guardian 1 g/91234567", "rate 1 r/40.25", "level 1 l/Sec4",
                "schedule 1 d/Mon t/16:00", "subject 1 s/H2 Math")) {
            setup.execute(command);
        }
        Person complete = model.getAddressBook().getPersonList().getFirst();
        ModelManager restarted = restart(storage);
        Person target = restarted.getAddressBook().getPersonList().getFirst();
        Person other = restarted.getAddressBook().getPersonList().getLast();
        assertEquals(complete, target);
        restarted.selectPerson(other.getId());
        byte[] before = Files.readAllBytes(storage.getAddressBookFilePath());
        AtomicInteger writes = new AtomicInteger();
        JsonAddressBookStorage counted = new JsonAddressBookStorage(storage.getAddressBookFilePath()) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook state) throws IOException {
                writes.incrementAndGet();
                super.saveAddressBook(state);
            }
        };
        Logic logic = logic(restarted, counted);

        assertEquals("Subject for Alex Tan is already H2 Math.",
                logic.execute("subject 1 s/\u00a0h2\u2002MATH ").getFeedbackToUser());
        assertEquals(0, writes.get());
        assertArrayEquals(before, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertSame(target, restarted.getAddressBook().getPersonList().getFirst());
        assertEquals(other.getId(), restarted.getSelectedPersonId());

        assertEquals("Subject updated for Alex Tan: H2 Math -> English Language.",
                logic.execute("subject 1 s/English Language").getFeedbackToUser());
        Person updated = restarted.getAddressBook().getPersonList().getFirst();
        assertEquals(complete.withStudentFields(complete.getStudentFields()
                .with(Subject.FIELD, new Subject("English Language"))), updated);
        assertSame(other, restarted.getAddressBook().getPersonList().getLast());
        assertEquals(target.getId(), restarted.getSelectedPersonId());
        assertEquals(1, writes.get());
        assertEquals(restarted.getAddressBook(), restart(storage).getAddressBook());

        logic.execute("level 1 l/JC1");
        logic.execute("schedule 1 d/Fri t/09:30");
        assertEquals("English Language", restart(storage).getAddressBook().getPersonList().getFirst()
                .getStudentFields().get(Subject.FIELD).orElseThrow().value);
    }

    @Test
    public void legacyMissingAndNullSubject_canBeAssignedWithoutLosingLegacyData() throws Exception {
        for (String subjectProperty : List.of("", ",\"subject\":null")) {
            Path file = directory.resolve("legacy.json");
            Files.writeString(file, """
                    {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Clementi Ave",
                    "email":"alex@example.com","tags":["legacy"],"guardianPhone":"+6591234567",
                    "hourlyRate":"40.25","futureField":{"nested":[1,"keep"]}%s}]}
                    """.formatted(subjectProperty));
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
            ModelManager model = restart(storage);
            Person original = model.getAddressBook().getPersonList().getFirst();
            assertTrue(original.getStudentFields().get(Subject.FIELD).isEmpty());
            assertEquals(original.getId(), restart(storage).getAddressBook().getPersonList().getFirst().getId());

            assertEquals("Subject set for Alex Tan: Physics & Chemistry.",
                    logic(model, storage).execute("subject 1 s/Physics & Chemistry").getFeedbackToUser());

            Person loaded = restart(storage).getAddressBook().getPersonList().getFirst();
            assertEquals(original.withStudentFields(original.getStudentFields()
                    .with(Subject.FIELD, new Subject("Physics & Chemistry"))), loaded);
            assertTrue(Files.readString(file).contains("\"id\""));
        }
    }

    @Test
    public void handEditedSubject_normalizesOnReadAndUnreadableValuesCanBeReplaced() throws Exception {
        for (String stored : List.of("\"  Ｈ２   Math  \"", "\"@@\"", "12", "{\"value\":\"Math\"}")) {
            Path file = directory.resolve("edited.json");
            Files.writeString(file, """
                    {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Clementi Ave",
                    "subject":%s,"futureField":{"keep":true}}]}
                    """.formatted(stored));
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
            ModelManager model = restart(storage);
            Person original = model.getAddressBook().getPersonList().getFirst();
            Logic logic = logic(model, storage);
            if (stored.contains("Ｈ２")) {
                byte[] bytes = Files.readAllBytes(file);
                assertEquals("H2 Math", original.getStudentFields().get(Subject.FIELD).orElseThrow().value);
                assertEquals("Subject for Alex Tan is already H2 Math.",
                        logic.execute("subject 1 s/h2 math").getFeedbackToUser());
                assertArrayEquals(bytes, Files.readAllBytes(file));
                assertSame(original, model.getAddressBook().getPersonList().getFirst());
            } else {
                assertThrows(IllegalArgumentException.class, () -> original.getStudentFields().get(Subject.FIELD));
            }
            String expected = stored.contains("Ｈ２")
                    ? "Subject updated for Alex Tan: H2 Math -> English Language."
                    : "Subject set for Alex Tan: English Language.";
            assertEquals(expected, logic.execute("subject 1 s/English Language").getFeedbackToUser());
            assertEquals(original.withStudentFields(original.getStudentFields()
                    .with(Subject.FIELD, new Subject("English Language"))),
                    restart(storage).getAddressBook().getPersonList().getFirst());
        }
    }

    @Test
    public void failedReplacement_filteredStudent_preservesStateAndCleansTemporaryFile() throws Exception {
        ModelManager model = new ModelManager();
        Person first = new PersonBuilder().withName("First Student").build();
        Person target = new PersonBuilder().withName("Alex Tan").build().withStudentFields(
                new StudentFields().with(Subject.FIELD, new Subject("H2 Math")));
        model.addPerson(first);
        model.addPerson(target);
        model.updateFilteredPersonList(person -> person.getId().equals(target.getId()));
        model.selectPerson(target.getId());
        var predicate = model.getPersonPredicate();
        Path file = directory.resolve("students.json");
        new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
        byte[] before = Files.readAllBytes(file);
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path destination) throws IOException {
                assertTrue(Files.readString(temporary).contains("English Language"));
                throw new IOException("Injected F06 atomic replacement failure");
            }
        };
        Logic logic = logic(model, new JsonAddressBookStorage(file, writer));

        CommandException failure = assertThrows(CommandException.class, () -> {
            logic.execute("subject 1 s/English Language");
        });
        assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());

        assertArrayEquals(before, Files.readAllBytes(file));
        assertSame(first, model.getAddressBook().getPersonList().getFirst());
        assertSame(target, model.getAddressBook().getPersonList().getLast());
        assertSame(target, model.getFilteredPersonList().getFirst());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(target.getId(), model.getSelectedPersonId());
        try (var files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    private ModelManager restart(JsonAddressBookStorage storage) throws Exception {
        return new ModelManager(storage.readAddressBook().orElseThrow(), new UserPrefs());
    }

    private Logic logic(ModelManager model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
