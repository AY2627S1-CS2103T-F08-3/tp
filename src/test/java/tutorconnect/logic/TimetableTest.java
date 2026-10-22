package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.logic.commands.CommandResult;
import tutorconnect.logic.commands.TimetableView;
import tutorconnect.logic.parser.TutorConnectParser;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

public class TimetableTest {
    @TempDir
    private Path directory;

    @Test
    public void parse_allAliasesAndUnicodeSpaces_useCanonicalDay() throws Exception {
        TutorConnectParser parser = new TutorConnectParser();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (String alias : List.of(day.name(), day.name().substring(0, 3).toLowerCase(Locale.ROOT))) {
                CommandResult result = parser.parseCommand("  timetable   d/\u3000" + alias + "  ")
                        .execute(new ModelManager());
                assertEquals(Optional.of(day), result.getTimetableView().orElseThrow().day());
            }
        }
        assertEquals(Optional.of(DayOfWeek.MONDAY), parser.parseCommand("timetable d/Ｍｏｎ")
                .execute(new ModelManager()).getTimetableView().orElseThrow().day());
    }

    @Test
    public void execute_readOnlyAndErrors_preserveModelFilterSelectionAndNeverSave() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy").build());
        model.addStudent(new StudentBuilder().withName("Zoe").build());
        var selected = model.getFilteredStudentList().getFirst();
        model.updateFilteredStudentList(student -> student.getId().equals(selected.getId()));
        model.selectStudent(selected.getId());
        var predicate = model.getStudentPredicate();
        var before = List.copyOf(model.getStudentDirectory().getStudentList());
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json")) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory state) {
                throw new AssertionError("Timetable commands must not save");
            }
        };
        Logic logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        assertEquals("Showing weekly timetable.", logic.execute("timetable").getFeedbackToUser());
        assertEquals("Showing timetable for Monday.", logic.execute("timetable d/mon").getFeedbackToUser());
        String[][] errors = {
            {"Timetable", Messages.MESSAGE_UNKNOWN_COMMAND},
            {"timetable x/Mon", "Error: Unknown parameter: x/."},
            {"timetable D/Mon", "Error: Unknown parameter: D/."},
            {"timetable Mon", "Error: Unexpected text after command."},
            {"timetable d/Mon d/Tue", "Error: Parameter d/ may be specified only once."},
            {"timetable d/ d/Tue", "Error: Parameter d/ may be specified only once."},
            {"timetable d/", "Error: Invalid day. Use Monday-Sunday or Mon-Sun."},
            {"timetable d/Mo", "Error: Invalid day. Use Monday-Sunday or Mon-Sun."},
            {"timetable d/Mon x/", "Error: Unknown parameter: x/."},
            {"timetable d/Mon extra", "Error: Unexpected text after command."},
            {"\ntimetable", "Error: Unexpected text after command."},
            {"timetable d/Mon\n", "Error: Unexpected text after command."}
        };
        for (String[] error : errors) {
            assertEquals(error[1], assertThrows(ParseException.class, () -> logic.execute(error[0])).getMessage());
        }
        assertEquals(before, model.getStudentDirectory().getStudentList());
        assertEquals(List.of(selected), model.getFilteredStudentList());
        assertSame(selected, model.getFilteredStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(selected.getId(), model.getSelectedStudentId());
    }

    @Test
    public void result_distinguishesWeekDayAndNoRequest() {
        var week = CommandResult.timetable("View", new TimetableView(Optional.empty()));
        var monday = CommandResult.timetable("View", new TimetableView(Optional.of(DayOfWeek.MONDAY)));
        assertNotEquals(week, monday);
        assertNotEquals(week, new CommandResult("View"));
        assertEquals(week, CommandResult.timetable("View", new TimetableView(Optional.empty())));
        assertEquals(week.hashCode(), CommandResult.timetable("View", new TimetableView(Optional.empty())).hashCode());
        assertThrows(NullPointerException.class, () -> new TimetableView(null));
    }
}
