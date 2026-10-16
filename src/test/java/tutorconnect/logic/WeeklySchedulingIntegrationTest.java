package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.Assert.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;
import static tutorconnect.testutil.TypicalStudents.BOB;

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
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;

public class WeeklySchedulingIntegrationTest {
    @TempDir
    public Path directory;

    private ModelManager model;
    private LogicManager logic;
    private RecordingWriter writer;
    private JsonStudentDirectoryStorage storage;
    private Path file;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        model.addStudent(ALICE);
        model.addStudent(BOB);
        file = directory.resolve("students.json");
        writer = new RecordingWriter();
        storage = new JsonStudentDirectoryStorage(file, writer);
        storage.saveStudentDirectory(model.getStudentDirectory());
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
        Student loaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
        assertEquals(new WeeklySlot(DayOfWeek.SATURDAY, LocalTime.of(9, 30)),
                WeeklySlotField.get(loaded).orElseThrow());
        assertEquals(ALICE.getId(), loaded.getId());
        assertEquals(ALICE.getId(), model.getSelectedStudentId());
        assertEquals(List.of(ALICE.getId(), BOB.getId()),
                model.getStudentDirectory().getStudentList().stream().map(Student::getId).toList());
    }

    @Test
    public void execute_normalizedNoChange_skipsSaveAndPreservesViewAndSelection() throws Exception {
        logic.execute("schedule 1 d/Tuesday t/19:00");
        model.selectStudent(BOB.getId());
        Student before = model.getFilteredStudentList().getFirst();
        String disk = Files.readString(file);
        var predicate = model.getStudentPredicate();
        AtomicInteger changes = observeRegister();
        writer.writes = 0;
        writer.failure = "move";
        assertEquals("Weekly lesson for " + ALICE.getName() + " is already Tuesday 19:00.",
                logic.execute("schedule １ t/１９：００ d/ｔＵｅ").getFeedbackToUser());
        assertEquals(0, writer.writes);
        assertEquals(0, changes.get());
        assertSame(before, model.getFilteredStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(BOB.getId(), model.getSelectedStudentId());
        assertEquals(disk, Files.readString(file));
    }

    @Test
    public void execute_failuresDuringWriteOrMove_preserveBothOldComponentsOrBothUnset() throws Exception {
        for (boolean existing : List.of(false, true)) {
            if (existing) {
                logic.execute("schedule 1 d/Tue t/19:00");
            }
            for (String failure : List.of("write", "move")) {
                model.selectStudent(BOB.getId());
                Student before = model.getFilteredStudentList().getFirst();
                String disk = Files.readString(file);
                var predicate = model.getStudentPredicate();
                AtomicInteger changes = observeRegister();
                writer.failure = failure;
                writer.beforeWrite = () -> {
                    assertSame(before, model.getFilteredStudentList().getFirst());
                    assertEquals(BOB.getId(), model.getSelectedStudentId());
                };
                assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_FAILURE, ()
                    -> logic.execute("schedule 1 d/Sat t/09:30"));
                assertSame(before, model.getFilteredStudentList().getFirst());
                assertSame(predicate, model.getStudentPredicate());
                assertEquals(BOB.getId(), model.getSelectedStudentId());
                assertEquals(0, changes.get());
                assertEquals(disk, Files.readString(file));
                assertEquals(WeeklySlotField.get(before),
                        WeeklySlotField.get(model.getFilteredStudentList().getFirst()));
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
        Student before = model.getFilteredStudentList().getFirst();
        String disk = Files.readString(file);
        writer.writes = 0;
        assertThrows(ParseException.class, WeeklySlot.MESSAGE_INVALID_DAY, ()
            -> logic.execute("schedule 999 t/24:00 d/Funday"));
        assertThrows(ParseException.class, WeeklySlot.MESSAGE_INVALID_TIME, ()
            -> logic.execute("schedule 999 d/Sun t/12:60"));
        assertThrows(CommandException.class, "Error: No student exists at index 999.", ()
            -> logic.execute("schedule 999 d/Sun t/23:59"));
        assertSame(before, model.getFilteredStudentList().getFirst());
        assertEquals(before.getId(), model.getSelectedStudentId());
        assertEquals(0, writer.writes);
        assertEquals(disk, Files.readString(file));
    }

    @Test
    public void execute_filteredIndexAndOverlap_persistAndDeleteAttachedSlot() throws Exception {
        logic.execute("schedule 1 d/Sun t/23:59");
        model.updateFilteredStudentList(student -> student.getId().equals(BOB.getId()));
        logic.execute("schedule 1 d/Sun t/23:59");
        assertEquals(BOB.getId(), model.getSelectedStudentId());
        for (Student student : storage.readStudentDirectory().orElseThrow().getStudentList()) {
            assertEquals(new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(23, 59)),
                    WeeklySlotField.get(student).orElseThrow());
        }
        logic.execute("delete 1");
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertEquals(null, model.getSelectedStudentId());
        assertEquals(List.of(ALICE.getId()),
                storage.readStudentDirectory().orElseThrow().getStudentList().stream().map(Student::getId).toList());
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
                        Student loaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
                        assertEquals(new WeeklySlot(day, LocalTime.parse(time)),
                                WeeklySlotField.get(loaded).orElseThrow());
                        assertEquals(ALICE.getId(), loaded.getId());
                        assertEquals(ALICE.getPhone(), loaded.getPhone());
                        assertEquals(ALICE.getAddress(), loaded.getAddress());
                        model.selectStudent(BOB.getId());
                        String disk = Files.readString(file);
                        writer.writes = 0;
                        assertEquals("Weekly lesson for " + ALICE.getName() + " is already " + full + " " + time + ".",
                                logic.execute("schedule 1 t/" + time + " d/"
                                        + full.substring(0, 3).toUpperCase(Locale.ROOT)).getFeedbackToUser());
                        assertEquals(0, writer.writes);
                        assertEquals(disk, Files.readString(file));
                        assertEquals(BOB.getId(), model.getSelectedStudentId());
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
        assertEquals(List.of(BOB.getId()), logic.getWeeklySchedule().stream().map(Student::getId).toList());
        logic.execute("schedule 1 d/Tue t/19:00");
        List<java.util.UUID> insertionOrder = List.of(ALICE.getId(), BOB.getId());
        assertEquals(insertionOrder, logic.getWeeklySchedule().stream().map(Student::getId).toList());
        model.updateFilteredStudentList(student -> student.getId().equals(BOB.getId()));
        logic.execute("schedule 1 d/Mon t/00:00");
        assertEquals(List.of(BOB.getId()), model.getFilteredStudentList().stream().map(Student::getId).toList());
        assertEquals(List.of(BOB.getId(), ALICE.getId()),
                logic.getWeeklySchedule().stream().map(Student::getId).toList());
        logic.execute("list");
        assertEquals(insertionOrder, model.getFilteredStudentList().stream().map(Student::getId).toList());
        logic.execute("schedule 2 d/Tue t/19:00");
        logic.execute("list");
        assertEquals(insertionOrder, logic.getWeeklySchedule().stream().map(Student::getId).toList());
        assertEquals(insertionOrder,
                model.getStudentDirectory().getStudentList().stream().map(Student::getId).toList());
    }

    private AtomicInteger observeRegister() {
        AtomicInteger changes = new AtomicInteger();
        model.getFilteredStudentList().addListener((ListChangeListener<Student>) change -> changes.incrementAndGet());
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
