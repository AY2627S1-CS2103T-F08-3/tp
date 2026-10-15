package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;
import seedu.address.storage.AtomicJsonFile;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class WeeklySchedulingIntegrationTest {
    @TempDir
    public Path directory;

    private ModelManager model;
    private LogicManager logic;
    private RecordingWriter writer;
    private JsonAddressBookStorage storage;
    private Path file;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BOB);
        file = directory.resolve("students.json");
        writer = new RecordingWriter();
        storage = new JsonAddressBookStorage(file, writer);
        storage.saveAddressBook(model.getAddressBook());
        writer.writes = 0;
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    @Test
    public void execute_setReplaceAndReload_completePairAndExactMessages() throws Exception {
        assertEquals("Weekly lesson set for " + ALICE.getName() + ": Tuesday 19:00.",
                logic.execute("schedule 1 d/Tue t/19:00").getFeedbackToUser());
        assertEquals("Weekly lesson updated for " + ALICE.getName() + ": Tuesday 19:00 -> Saturday 09:30.",
                logic.execute("schedule 1 t/09:30 d/Sat").getFeedbackToUser());
        Person loaded = storage.readAddressBook().orElseThrow().getPersonList().getFirst();
        assertEquals(new WeeklySlot(DayOfWeek.SATURDAY, LocalTime.of(9, 30)),
                WeeklySlotField.get(loaded).orElseThrow());
        assertEquals(ALICE.getId(), loaded.getId());
        assertEquals(ALICE.getId(), model.getSelectedPersonId());
        assertEquals(List.of(ALICE.getId(), BOB.getId()),
                model.getAddressBook().getPersonList().stream().map(Person::getId).toList());
    }

    @Test
    public void execute_normalizedNoChange_skipsSaveAndPreservesViewAndSelection() throws Exception {
        logic.execute("schedule 1 d/Tuesday t/19:00");
        model.selectPerson(BOB.getId());
        Person before = model.getFilteredPersonList().getFirst();
        String disk = Files.readString(file);
        var predicate = model.getPersonPredicate();
        AtomicInteger changes = observeRegister();
        writer.writes = 0;
        writer.failure = "move";
        assertEquals("Weekly lesson for " + ALICE.getName() + " is already Tuesday 19:00.",
                logic.execute("schedule １ t/１９：００ d/ｔＵｅ").getFeedbackToUser());
        assertEquals(0, writer.writes);
        assertEquals(0, changes.get());
        assertSame(before, model.getFilteredPersonList().getFirst());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(BOB.getId(), model.getSelectedPersonId());
        assertEquals(disk, Files.readString(file));
    }

    @Test
    public void execute_failuresDuringWriteOrMove_preserveBothOldComponentsOrBothUnset() throws Exception {
        for (boolean existing : List.of(false, true)) {
            if (existing) {
                logic.execute("schedule 1 d/Tue t/19:00");
            }
            for (String failure : List.of("write", "move")) {
                model.selectPerson(BOB.getId());
                Person before = model.getFilteredPersonList().getFirst();
                String disk = Files.readString(file);
                var predicate = model.getPersonPredicate();
                AtomicInteger changes = observeRegister();
                writer.failure = failure;
                writer.beforeWrite = () -> {
                    assertSame(before, model.getFilteredPersonList().getFirst());
                    assertEquals(BOB.getId(), model.getSelectedPersonId());
                };
                assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_FAILURE, ()
                    -> logic.execute("schedule 1 d/Sat t/09:30"));
                assertSame(before, model.getFilteredPersonList().getFirst());
                assertSame(predicate, model.getPersonPredicate());
                assertEquals(BOB.getId(), model.getSelectedPersonId());
                assertEquals(0, changes.get());
                assertEquals(disk, Files.readString(file));
                assertEquals(WeeklySlotField.get(before),
                        WeeklySlotField.get(model.getFilteredPersonList().getFirst()));
                try (var files = Files.list(directory)) {
                    assertEquals(1, files.count());
                }
                writer.failure = "";
                writer.beforeWrite = () -> {};
            }
        }
    }

    @Test
    public void execute_invalidValues_precedeNonexistentIndexAndNeverChangeEitherComponent() throws Exception {
        logic.execute("schedule 1 d/Tue t/19:00");
        Person before = model.getFilteredPersonList().getFirst();
        String disk = Files.readString(file);
        writer.writes = 0;
        assertThrows(ParseException.class, WeeklySlot.MESSAGE_INVALID_DAY, ()
            -> logic.execute("schedule 999 t/24:00 d/Funday"));
        assertThrows(ParseException.class, WeeklySlot.MESSAGE_INVALID_TIME, ()
            -> logic.execute("schedule 999 d/Sun t/12:60"));
        assertThrows(CommandException.class, "Error: No student exists at index 999.", ()
            -> logic.execute("schedule 999 d/Sun t/23:59"));
        assertSame(before, model.getFilteredPersonList().getFirst());
        assertEquals(before.getId(), model.getSelectedPersonId());
        assertEquals(0, writer.writes);
        assertEquals(disk, Files.readString(file));
    }

    @Test
    public void execute_filteredIndexAndOverlap_persistAndDeleteAttachedSlot() throws Exception {
        logic.execute("schedule 1 d/Sun t/23:59");
        model.updateFilteredPersonList(person -> person.getId().equals(BOB.getId()));
        logic.execute("schedule 1 d/Sun t/23:59");
        assertEquals(BOB.getId(), model.getSelectedPersonId());
        for (Person person : storage.readAddressBook().orElseThrow().getPersonList()) {
            assertEquals(new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(23, 59)),
                    WeeklySlotField.get(person).orElseThrow());
        }
        logic.execute("delete 1");
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(null, model.getSelectedPersonId());
        assertEquals(List.of(ALICE.getId()),
                storage.readAddressBook().orElseThrow().getPersonList().stream().map(Person::getId).toList());
    }

    @Test
    public void execute_allDaysAtTimeBoundaries_roundTripAndAliasNoChangeAcrossLocales() throws Exception {
        Locale original = Locale.getDefault();
        try {
            for (Locale locale : List.of(Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("ar-SA"))) {
                Locale.setDefault(locale);
                for (DayOfWeek day : DayOfWeek.values()) {
                    String full = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                    for (String time : List.of("00:00", "23:59")) {
                        logic.execute("schedule 1 d/" + full + " t/" + time);
                        Person loaded = storage.readAddressBook().orElseThrow().getPersonList().getFirst();
                        assertEquals(new WeeklySlot(day, LocalTime.parse(time)),
                                WeeklySlotField.get(loaded).orElseThrow());
                        assertEquals(ALICE.getId(), loaded.getId());
                        assertEquals(ALICE.getPhone(), loaded.getPhone());
                        assertEquals(ALICE.getAddress(), loaded.getAddress());
                        model.selectPerson(BOB.getId());
                        String disk = Files.readString(file);
                        writer.writes = 0;
                        assertEquals("Weekly lesson for " + ALICE.getName() + " is already " + full + " " + time + ".",
                                logic.execute("schedule 1 t/" + time + " d/"
                                        + full.substring(0, 3).toUpperCase(Locale.ROOT)).getFeedbackToUser());
                        assertEquals(0, writer.writes);
                        assertEquals(disk, Files.readString(file));
                        assertEquals(BOB.getId(), model.getSelectedPersonId());
                    }
                }
            }
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    public void summary_schedulingOrderDoesNotReplaceInsertionOrder_evenAfterFilterAndReload() throws Exception {
        logic.execute("schedule 2 d/Tue t/19:00");
        assertEquals(List.of(BOB.getId()), logic.getWeeklySchedule().stream().map(Person::getId).toList());
        logic.execute("schedule 1 d/Tue t/19:00");
        List<java.util.UUID> insertionOrder = List.of(ALICE.getId(), BOB.getId());
        assertEquals(insertionOrder, logic.getWeeklySchedule().stream().map(Person::getId).toList());
        model.updateFilteredPersonList(person -> person.getId().equals(BOB.getId()));
        logic.execute("schedule 1 d/Mon t/00:00");
        assertEquals(List.of(BOB.getId()), model.getFilteredPersonList().stream().map(Person::getId).toList());
        assertEquals(List.of(BOB.getId(), ALICE.getId()),
                logic.getWeeklySchedule().stream().map(Person::getId).toList());
        logic.execute("list");
        assertEquals(insertionOrder, model.getFilteredPersonList().stream().map(Person::getId).toList());
        logic.execute("schedule 2 d/Tue t/19:00");
        logic.execute("list");
        assertEquals(insertionOrder, logic.getWeeklySchedule().stream().map(Person::getId).toList());
        assertEquals(insertionOrder, model.getAddressBook().getPersonList().stream().map(Person::getId).toList());
    }

    private AtomicInteger observeRegister() {
        AtomicInteger changes = new AtomicInteger();
        model.getFilteredPersonList().addListener((ListChangeListener<Person>) change -> changes.incrementAndGet());
        return changes;
    }

    private static class RecordingWriter extends AtomicJsonFile {
        private int writes;
        private String failure = "";
        private Runnable beforeWrite = () -> {};

        @Override
        public void write(Path destination, String json) throws IOException {
            writes++;
            beforeWrite.run();
            super.write(destination, json);
        }

        @Override
        protected void writeTemporary(Path temporary, String json) throws IOException {
            if (failure.equals("write")) {
                Files.writeString(temporary, "partial");
                throw new IOException("Injected disk-full failure");
            }
            super.writeTemporary(temporary, json);
        }

        @Override
        protected void replace(Path temporary, Path target) throws IOException {
            if (failure.equals("move")) {
                throw new IOException("Injected atomic-move failure");
            }
            super.replace(temporary, target);
        }
    }
}
