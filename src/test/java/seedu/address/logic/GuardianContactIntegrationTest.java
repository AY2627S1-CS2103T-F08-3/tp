package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.TextNode;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.ParserUtil;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.GuardianPhone;
import seedu.address.model.person.HourlyRate;
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

/** F04 acceptance gaps beyond the shared cross-feature integration tests. */
public class GuardianContactIntegrationTest {
    @TempDir
    private Path directory;
    private ModelManager model;
    private Logic logic;
    private JsonAddressBookStorage storage;
    private RecordingWriter writer;
    private Path file;

    @BeforeEach
    public void setUp() throws Exception {
        StudentFields fields = new StudentFields(Map.of("futureField", TextNode.valueOf("keep me")))
                .with(EducationLevel.FIELD, new EducationLevel("Sec4"))
                .with(Subject.FIELD, new Subject("H2 Math"))
                .with(HourlyRate.FIELD, ParserUtil.parseHourlyRate("80"))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alex Tan").withPhone("81234567")
                .build().withStudentFields(fields));
        model.addPerson(new PersonBuilder().withName("Nur Aisyah").withPhone("92345678").build());
        file = directory.resolve("students.json");
        writer = new RecordingWriter();
        storage = new JsonAddressBookStorage(file, writer);
        storage.saveAddressBook(model.getAddressBook());
        writer.writes = 0;
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    @Test
    public void execute_sharedFamilyAndStudentPhones_preserveCompleteProfilesAfterRestart() throws Exception {
        Person original = model.getAddressBook().getPersonList().getFirst();
        Person sibling = model.getAddressBook().getPersonList().getLast();
        assertEquals("Guardian contact set for Alex Tan: +6581234567.",
                logic.execute("guardian 1 g/+65 8123 4567").getFeedbackToUser());
        // A guardian may be the primary contact; siblings may share the same guardian.
        assertEquals("Guardian contact set for Nur Aisyah: +6581234567.",
                logic.execute("guardian 2 g/81234567").getFeedbackToUser());
        assertEquals("Guardian contact updated for Alex Tan: +6581234567 -> +6567778899.",
                logic.execute("guardian 1 g/+65-6777-8899").getFeedbackToUser());
        assertEquals(3, writer.writes);
        assertEquals(original.withGuardianPhone(new GuardianPhone("+6567778899")),
                model.getAddressBook().getPersonList().getFirst());
        assertEquals(sibling.withGuardianPhone(new GuardianPhone("+6581234567")),
                model.getAddressBook().getPersonList().getLast());
        ModelManager restarted = new ModelManager(new JsonAddressBookStorage(file).readAddressBook().orElseThrow(),
                new UserPrefs());
        assertEquals(model.getAddressBook(), restarted.getAddressBook());
        assertEquals(List.of(original.getId(), sibling.getId()),
                restarted.getAddressBook().getPersonList().stream().map(Person::getId).toList());
        assertEquals(original.getId(), model.getSelectedPersonId());
        String json = Files.readString(file);
        assertEquals(2, json.split("\"guardianPhone\"", -1).length - 1);
    }

    @Test
    public void execute_firstAssignmentAndReplacement_writeFailuresDoNotPublish() throws Exception {
        for (boolean existing : List.of(false, true)) {
            if (existing) {
                logic.execute("guardian 1 g/91234567");
            }
            // Keep the target at displayed index 1 but preserve a different selected row.
            model.updateFilteredPersonList(person -> person.getName().fullName.contains("a"));
            model.selectPerson(model.getFilteredPersonList().getLast().getId());
            Person before = model.getFilteredPersonList().getFirst();
            var selected = model.getSelectedPersonId();
            var predicate = model.getPersonPredicate();
            String disk = Files.readString(file);
            writer.beforeWrite = () -> {
                assertSame(before, model.getFilteredPersonList().getFirst());
                assertEquals(selected, model.getSelectedPersonId());
            };
            for (boolean failMove : List.of(false, true)) {
                writer.failWrite = !failMove;
                writer.failMove = failMove;
                CommandException error = assertThrows(CommandException.class, ()
                    -> logic.execute("guardian 1 g/82345678"));
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, error.getMessage());
                assertSame(before, model.getFilteredPersonList().getFirst());
                assertSame(predicate, model.getPersonPredicate());
                assertEquals(selected, model.getSelectedPersonId());
                assertEquals(disk, Files.readString(file));
                try (var files = Files.list(directory)) {
                    assertEquals(List.of(file), files.toList());
                }
            }
            writer.failWrite = false;
            writer.failMove = false;
            writer.beforeWrite = () -> {};
        }
    }

    @Test
    public void execute_invalidValuesBeforeMissingRow_preserveStateAndAllowRecovery() throws Exception {
        Person target = model.getAddressBook().getPersonList().getLast();
        model.updateFilteredPersonList(person -> person.getId().equals(target.getId()));
        model.selectPerson(target.getId());
        var predicate = model.getPersonPredicate();
        String disk = Files.readString(file);
        for (String command : List.of("guardian 999 g/bad", "guardian 1 g/", "guardian 1 g/81234567g/91234567")) {
            ParseException error = assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(ParserUtil.MESSAGE_INVALID_GUARDIAN_PHONE, error.getMessage());
        }
        for (String index : List.of("2", "9".repeat(100))) {
            CommandException error = assertThrows(CommandException.class, ()
                -> logic.execute("guardian " + index + " g/81234567"));
            assertEquals("Error: No student exists at index " + index + ".", error.getMessage());
        }
        assertEquals(0, writer.writes);
        assertSame(target, model.getFilteredPersonList().getFirst());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(target.getId(), model.getSelectedPersonId());
        assertEquals(disk, Files.readString(file));
        assertEquals("Guardian contact set for Nur Aisyah: +6581234567.",
                logic.execute("guardian 1 g/81234567").getFeedbackToUser());
        Person updated = model.getFilteredPersonList().getFirst();
        disk = Files.readString(file);
        writer.failWrite = true;
        assertEquals("Guardian contact for Nur Aisyah is already +6581234567.",
                logic.execute("guardian 1 g/+65-8123-4567").getFeedbackToUser());
        assertEquals(1, writer.writes);
        assertSame(updated, model.getFilteredPersonList().getFirst());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(disk, Files.readString(file));
    }

    private static class RecordingWriter extends AtomicJsonFile {
        private int writes;
        private boolean failWrite;
        private boolean failMove;
        private Runnable beforeWrite = () -> {};

        @Override
        public void write(Path destination, String json) throws IOException {
            writes++;
            beforeWrite.run();
            super.write(destination, json);
        }

        @Override
        protected void writeTemporary(Path temporary, String json) throws IOException {
            if (failWrite) {
                Files.writeString(temporary, "partial");
                throw new IOException("Injected partial write failure");
            }
            super.writeTemporary(temporary, json);
        }

        @Override
        protected void replace(Path temporary, Path destination) throws IOException {
            if (failMove) {
                throw new IOException("Injected atomic move failure");
            }
            super.replace(temporary, destination);
        }
    }
}
