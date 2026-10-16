package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static tutorconnect.testutil.Assert.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;
import static tutorconnect.testutil.TypicalStudents.BOB;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.VisibleIndex;
import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;

public class ScheduleCommandTest {
    private final Model model = new ModelManager();
    private final WeeklySlot tuesday = new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(19, 0));

    @Test
    public void execute_setThenReplace_exactMessagesAndCompletePair() throws Exception {
        model.addStudent(ALICE);
        String name = ALICE.getName().toString();
        assertEquals("Weekly lesson set for " + name + ": Tuesday 19:00.",
                new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model).getFeedbackToUser());
        WeeklySlot saturday = new WeeklySlot(DayOfWeek.SATURDAY, LocalTime.of(9, 30));
        assertEquals("Weekly lesson updated for " + name + ": Tuesday 19:00 -> Saturday 09:30.",
                new ScheduleCommand(VisibleIndex.parse("1"), saturday).execute(model).getFeedbackToUser());
        assertEquals(saturday, WeeklySlotField.get(model.getFilteredStudentList().get(0)).orElseThrow());
        assertEquals(ALICE.getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_noChange_preservesStudentObjectFilterAndSelection() throws Exception {
        model.addStudent(ALICE);
        model.addStudent(BOB);
        new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model);
        model.selectStudent(BOB.getId());
        Student original = model.getFilteredStudentList().get(0);
        var predicate = model.getStudentPredicate();
        assertEquals("Weekly lesson for " + ALICE.getName() + " is already Tuesday 19:00.",
                new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model).getFeedbackToUser());
        assertSame(original, model.getFilteredStudentList().get(0));
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(BOB.getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_filteredIndex_preservesRegisterPositionAndUnrelatedFields() throws Exception {
        var node = JsonNodeFactory.instance;
        Student target = BOB.withStudentFields(new StudentFields(Map.of("subject", node.textNode("Math/Science"),
                "guardianPhone", node.textNode("+6581234567"), "hourlyRate", node.textNode("65.00"),
                "educationLevel", node.textNode("Secondary 2"))));
        model.addStudent(ALICE);
        model.addStudent(target);
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        var predicate = model.getStudentPredicate();
        new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model);
        Student updated = model.getFilteredStudentList().get(0);
        assertEquals(target.getId(), updated.getId());
        assertEquals(target.getName(), updated.getName());
        assertEquals(target.getPhone(), updated.getPhone());
        assertEquals(target.getAddress(), updated.getAddress());
        assertEquals(target.getEmail(), updated.getEmail());
        assertEquals(target.getTags(), updated.getTags());
        target.getStudentFields().toStorage().forEach((key, value) ->
                assertEquals(value, updated.getStudentFields().toStorage().get(key)));
        assertEquals(List.of(ALICE.getId(), BOB.getId()),
                model.getStudentDirectory().getStudentList().stream().map(Student::getId).toList());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(target.getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_identicalSlotsForDifferentStudents_acceptsOverlap() throws Exception {
        model.addStudent(ALICE);
        model.addStudent(BOB);
        new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model);
        new ScheduleCommand(VisibleIndex.parse("2"), tuesday).execute(model);
        for (Student student : model.getStudentDirectory().getStudentList()) {
            assertEquals(tuesday, WeeklySlotField.get(student).orElseThrow());
        }
    }

    @Test
    public void execute_nonexistentAndOversizedIndex_preservesState() throws Exception {
        model.addStudent(ALICE);
        for (String index : List.of("2", "99999999999999999999999999999999999999")) {
            VisibleIndex parsed = VisibleIndex.parse(index);
            assertThrows(CommandException.class, "Error: No student exists at index " + index + ".", ()
                -> new ScheduleCommand(parsed, tuesday).execute(model));
            assertSame(ALICE, model.getFilteredStudentList().get(0));
            assertEquals(ALICE.getId(), model.getSelectedStudentId());
        }
    }
}
