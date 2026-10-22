package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.Subject;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** Acceptance across real parsing, staged commands, displayed UUID resolution and JSON persistence. */
class StudentDiscoveryIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void queries_replaceFullRegisterViewWithoutSavingAndKeepScheduleIndependent() throws Exception {
        Fixture fixture = fixture();
        List<Student> all = List.copyOf(fixture.model.getStudentDirectory().getStudentList());
        List<Student> schedule = List.copyOf(fixture.logic.getWeeklySchedule());
        String stored = Files.readString(fixture.path);
        fixture.logic.execute("filter l/p1 s/ｍＡＴＨ");
        assertEquals(all.subList(1, 3), fixture.logic.getFilteredStudentList());
        fixture.logic.execute("find ｚＯ");
        assertEquals(List.of(all.getFirst()), fixture.logic.getFilteredStudentList());
        fixture.logic.execute("find ｊｏｓｅ\u0301 amY");
        assertEquals(all.subList(1, 3), fixture.logic.getFilteredStudentList());
        fixture.logic.execute("filter l/p2");
        assertEquals(List.of(all.getFirst()), fixture.logic.getFilteredStudentList());
        for (String query : List.of("find nobody", "filter s/Chemistry")) {
            assertEquals("0 student(s) listed!", fixture.logic.execute(query).getFeedbackToUser());
            assertEquals(List.of(), fixture.logic.getFilteredStudentList());
            assertEquals(schedule, fixture.logic.getWeeklySchedule());
        }
        fixture.logic.execute("list");
        assertEquals(all, fixture.logic.getFilteredStudentList());
        assertNull(fixture.logic.getSelectedStudentId());
        assertEquals(all, fixture.model.getStudentDirectory().getStudentList());
        assertEquals(schedule, fixture.logic.getWeeklySchedule());
        assertEquals(stored, Files.readString(fixture.path));
        assertEquals(0, fixture.writes.get());
    }

    @Test
    void indexedMutations_afterEitherQuery_targetDisplayedUuidAndPreserveOtherStudents() throws Exception {
        for (String query : List.of("find an", "filter l/p1 s/Math")) {
            for (String command : List.of("edit 1 a/New Road", "guardian 1 g/91234567", "rate 1 r/80",
                    "level 1 l/p2", "subject 1 s/Physics", "schedule 1 d/Fri t/18:00", "delete 1")) {
                Fixture fixture = fixture();
                List<Student> before = List.copyOf(fixture.model.getStudentDirectory().getStudentList());
                Student target = before.get(1);
                fixture.logic.execute(query);
                assertEquals(target.getId(), fixture.logic.getFilteredStudentList().getFirst().getId());
                fixture.logic.execute(command);
                List<Student> after = fixture.model.getStudentDirectory().getStudentList();
                assertEquals(before.getFirst(), after.getFirst(), command);
                assertEquals(before.getLast(), after.getLast(), command);
                if (command.startsWith("delete")) {
                    assertEquals(2, after.size());
                    assertFalse(after.stream().anyMatch(student -> student.getId().equals(target.getId())));
                } else {
                    assertEquals(target.getId(), after.get(1).getId(), command);
                    assertFalse(target.equals(after.get(1)), command);
                }
                if (query.startsWith("filter") && (command.startsWith("level") || command.startsWith("subject"))) {
                    assertEquals(List.of(before.getLast()), fixture.logic.getFilteredStudentList());
                    assertNull(fixture.logic.getSelectedStudentId());
                }
                assertEquals(1, fixture.writes.get(), command);
                assertEquals(after, new JsonStudentDirectoryStorage(fixture.path)
                        .readStudentDirectory().orElseThrow().getStudentList());
            }
        }
    }

    @Test
    void invalidQueriesAndMutations_preserveViewSelectionAndBytes() throws Exception {
        Fixture fixture = fixture();
        fixture.logic.execute("filter l/p1 s/Math");
        fixture.logic.selectStudent(fixture.logic.getFilteredStudentList().getFirst().getId());
        var selected = fixture.logic.getSelectedStudentId();
        var predicate = fixture.model.getStudentPredicate();
        List<Student> visible = List.copyOf(fixture.logic.getFilteredStudentList());
        List<Student> records = List.copyOf(fixture.model.getStudentDirectory().getStudentList());
        String stored = Files.readString(fixture.path);
        for (String command : List.of("find", "find \u200b", "filter", "filter l/", "filter s/",
                "filter x/Math", "filter s/Math s/Physics", "filter l/p9", "filter s/Math!",
                "edit 1", "guardian 1 g/no", "rate 1 r/no", "level 1 l/no", "subject 1 s/!",
                "schedule 1 d/no t/12:00", "delete 0")) {
            assertThrows(ParseException.class, () -> fixture.logic.execute(command), command);
            assertEquals(visible, fixture.logic.getFilteredStudentList(), command);
            assertEquals(selected, fixture.logic.getSelectedStudentId(), command);
            assertSame(predicate, fixture.model.getStudentPredicate(), command);
        }
        for (String command : List.of("edit 3 a/Road", "guardian 3 g/91234567", "rate 3 r/80",
                "level 3 l/p2", "subject 3 s/Physics", "schedule 3 d/Fri t/18:00", "delete 3")) {
            assertThrows(CommandException.class, () -> fixture.logic.execute(command), command);
            assertEquals(visible, fixture.logic.getFilteredStudentList(), command);
            assertEquals(selected, fixture.logic.getSelectedStudentId(), command);
        }
        assertEquals(records, fixture.model.getStudentDirectory().getStudentList());
        assertEquals(stored, Files.readString(fixture.path));
        assertEquals(0, fixture.writes.get());
    }

    private Fixture fixture() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(student("Zoe Lim", "p2", "Physics", "Mon"));
        model.addStudent(student("José Tan", "p1", "Math", "Wed"));
        model.addStudent(student("Amy Tan", "p1", "Math", "Tue"));
        Path path = directory.resolve("students.json");
        new JsonStudentDirectoryStorage(path).saveStudentDirectory(model.getStudentDirectory());
        AtomicInteger writes = new AtomicInteger();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(path) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) throws IOException {
                writes.incrementAndGet();
                super.saveStudentDirectory(proposed);
            }
        };
        Logic logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        return new Fixture(model, logic, path, writes);
    }

    private Student student(String name, String level, String subject, String day) {
        Student student = new StudentBuilder().withName(name).build();
        return student.withStudentFields(student.getStudentFields()
                .with(EducationLevel.FIELD, new EducationLevel(level))
                .with(Subject.FIELD, new Subject(subject))
                .with(WeeklySlotField.INSTANCE,
                        new WeeklySlot(WeeklySlot.parseDay(day), WeeklySlot.parseTime("12:00"))));
    }

    private record Fixture(ModelManager model, Logic logic, Path path, AtomicInteger writes) {}
}
