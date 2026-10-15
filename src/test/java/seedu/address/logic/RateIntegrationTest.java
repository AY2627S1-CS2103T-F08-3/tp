package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
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
import seedu.address.logic.parser.ParserUtil;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.HourlyRate;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** F07 acceptance through the real parser, displayed model and atomic JSON storage. */
public class RateIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void setReplaceAndEquivalentAmounts_useExactFeedbackAndSaveOnlyChanges() throws Exception {
        ModelManager model = model();
        Person original = model.getAddressBook().getPersonList().getFirst();
        Person other = model.getAddressBook().getPersonList().getLast();
        AtomicInteger writes = new AtomicInteger();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook proposed) throws IOException {
                writes.incrementAndGet();
                super.saveAddressBook(proposed);
            }
        };
        Logic logic = logic(model, storage);
        assertEquals("Hourly rate set for Alex Tan: S$1.00.",
                logic.execute("rate 1 r/1").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$1.00 -> S$1000.00.",
                logic.execute("rate 1 r/1000").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$1000.00 -> S$19.99.",
                logic.execute("rate 1 r/19.99").getFeedbackToUser());
        assertEquals("Hourly rate updated for Alex Tan: S$19.99 -> S$40.00.",
                logic.execute("rate 1 r/40").getFeedbackToUser());
        Person updated = model.getAddressBook().getPersonList().getFirst();
        assertEquals(original.getId(), model.getSelectedPersonId());
        assertSame(other, model.getAddressBook().getPersonList().getLast());
        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getPhone(), updated.getPhone());
        assertEquals(original.getAddress(), updated.getAddress());
        assertEquals(original.getEmail(), updated.getEmail());
        assertEquals(original.getTags(), updated.getTags());
        var unrelated = updated.getStudentFields().toStorage();
        unrelated.remove("hourlyRate");
        assertEquals(original.getStudentFields().toStorage(), unrelated);
        assertEquals(updated, storage.readAddressBook().orElseThrow().getPersonList().getFirst());

        model.selectPerson(other.getId());
        var predicate = model.getPersonPredicate();
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        for (String amount : List.of("40", "40.0", "40.00")) {
            assertEquals("Hourly rate for Alex Tan is already S$40.00.",
                    logic.execute("rate 1 r/" + amount).getFeedbackToUser());
            assertSame(updated, model.getAddressBook().getPersonList().getFirst());
            assertSame(predicate, model.getPersonPredicate());
            assertEquals(other.getId(), model.getSelectedPersonId());
            assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
        }
        assertEquals(4, writes.get());
    }

    @Test
    public void filteredIndex_updatesDisplayedStudentAndRejectsHiddenStoragePosition() throws Exception {
        ModelManager model = model();
        Person hidden = model.getAddressBook().getPersonList().getFirst();
        Person displayed = model.getAddressBook().getPersonList().getLast();
        Logic logic = logic(model, new JsonAddressBookStorage(directory.resolve("students.json")));
        logic.execute("find Other");
        var predicate = model.getPersonPredicate();
        assertEquals(List.of(displayed), model.getFilteredPersonList());
        logic.execute("rate 1 r/80.25");
        assertSame(hidden, model.getAddressBook().getPersonList().getFirst());
        assertEquals(displayed.getId(), model.getFilteredPersonList().getFirst().getId());
        assertEquals(new HourlyRate(new BigDecimal("80.25")),
                model.getFilteredPersonList().getFirst().getHourlyRate().orElseThrow());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(displayed.getId(), model.getSelectedPersonId());
        for (String index : List.of("2", "9".repeat(100))) {
            CommandException failure = assertThrows(CommandException.class, () ->
                    logic.execute("rate " + index + " r/40"));
            assertEquals("Error: No student exists at index " + index + ".", failure.getMessage());
        }
        logic.execute("find Nobody");
        assertThrows(CommandException.class, () -> logic.execute("rate 1 r/40"));
        ParseException invalidRate = assertThrows(ParseException.class, () -> logic.execute("rate 99 r/bad"));
        assertEquals(ParserUtil.MESSAGE_INVALID_HOURLY_RATE, invalidRate.getMessage());
    }

    @Test
    public void invalidInput_preservesFileObjectsFilterAndSelection() throws Exception {
        ModelManager model = model();
        Person target = model.getAddressBook().getPersonList().getLast();
        model.updateFilteredPersonList(person -> person.getId().equals(target.getId()));
        model.selectPerson(target.getId());
        var predicate = model.getPersonPredicate();
        List<Person> before = List.copyOf(model.getAddressBook().getPersonList());
        JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json"));
        storage.saveAddressBook(model.getAddressBook());
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        Logic logic = logic(model, storage);
        for (String command : List.of("rate 1 r/0", "rate 1 r/40.000", "rate 1 r/",
                "rate 1", "rate 1 r/40 r/50", "rate 01 r/40", "rate 1 x/40")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(before, model.getAddressBook().getPersonList());
            assertSame(target, model.getFilteredPersonList().getFirst());
            assertSame(predicate, model.getPersonPredicate());
            assertEquals(target.getId(), model.getSelectedPersonId());
            assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
        }
    }

    @Test
    public void failedFirstSetAndReplacement_rollBackAtWriteAndAtomicMove() throws Exception {
        for (boolean existingRate : List.of(false, true)) {
            for (boolean failReplace : List.of(false, true)) {
                ModelManager model = model();
                if (existingRate) {
                    Person target = model.getAddressBook().getPersonList().getFirst();
                    model.setPerson(target, target.withHourlyRate(new HourlyRate(new BigDecimal("40"))));
                }
                Person target = model.getAddressBook().getPersonList().getFirst();
                Person selected = model.getAddressBook().getPersonList().getLast();
                model.updateFilteredPersonList(person -> true);
                model.selectPerson(selected.getId());
                var predicate = model.getPersonPredicate();
                List<Person> before = List.copyOf(model.getAddressBook().getPersonList());
                Path file = directory.resolve("students.json");
                new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
                byte[] saved = Files.readAllBytes(file);
                AtomicJsonFile writer = new AtomicJsonFile() {
                    @Override
                    protected void writeTemporary(Path temporary, String json) throws IOException {
                        super.writeTemporary(temporary, json);
                        if (!failReplace) {
                            throw new IOException("Injected failure after temporary write");
                        }
                    }

                    @Override
                    protected void replace(Path temporary, Path destination) throws IOException {
                        throw new IOException("Injected atomic replacement failure");
                    }
                };
                Logic logic = logic(model, new JsonAddressBookStorage(file, writer));
                CommandException failure = assertThrows(CommandException.class, () -> logic.execute("rate 1 r/80"));
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());
                assertArrayEquals(saved, Files.readAllBytes(file));
                assertEquals(before, model.getAddressBook().getPersonList());
                assertSame(target, model.getAddressBook().getPersonList().getFirst());
                assertSame(selected, model.getAddressBook().getPersonList().getLast());
                assertSame(predicate, model.getPersonPredicate());
                assertEquals(selected.getId(), model.getSelectedPersonId());
                try (var files = Files.list(directory)) {
                    assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".student-save-")));
                }
            }
        }
    }

    @Test
    public void failedInitialSave_doesNotCreateRegisterOrPublishRate() {
        ModelManager model = model();
        Person original = model.getAddressBook().getPersonList().getFirst();
        Path file = directory.resolve("new.json");
        Logic logic = logic(model, new JsonAddressBookStorage(file, new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path destination) throws IOException {
                throw new IOException("Injected initial save failure");
            }
        }));
        assertThrows(CommandException.class, () -> logic.execute("rate 1 r/40"));
        assertFalse(Files.exists(file));
        assertSame(original, model.getAddressBook().getPersonList().getFirst());
        assertTrue(original.getHourlyRate().isEmpty());
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("guardianPhone", TextNode.valueOf("+6591234567"),
                "educationLevel", TextNode.valueOf("Secondary 1"), "subject", TextNode.valueOf("Mathematics"),
                "futureField", TextNode.valueOf("preserve me")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alex Tan").withTags("family").build().withStudentFields(fields));
        model.addPerson(new PersonBuilder().withName("Other Student").withPhone("91234567").build());
        return model;
    }

    private Logic logic(ModelManager model, JsonAddressBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
