package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorconnect.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.AcademicNote;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.StudentBuilder;

public class NoteCommandTest {

    private static final AcademicNote CALCULUS = new AcademicNote("Weak in calculus");

    private static ModelManager modelWithOneStudent() {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build());
        return model;
    }

    @Test
    public void execute_noNote_setsNoteAndSelectsStudent() throws Exception {
        ModelManager model = modelWithOneStudent();

        CommandResult result = new NoteCommand(INDEX_FIRST_STUDENT, CALCULUS).execute(model);

        assertEquals("Note set for Amy Bee.", result.getFeedbackToUser());
        assertEquals(Optional.of(1), result.getSelectedStudentIndex());
        Student student = model.getFilteredStudentList().getFirst();
        assertEquals(Optional.of(CALCULUS), student.getStudentFields().get(AcademicNote.FIELD));
        assertEquals(student.getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_differentNote_replacesTheNote() throws Exception {
        ModelManager model = modelWithOneStudent();
        new NoteCommand(INDEX_FIRST_STUDENT, CALCULUS).execute(model);

        CommandResult result = new NoteCommand(INDEX_FIRST_STUDENT, new AcademicNote("Short attention span"))
                .execute(model);

        assertEquals("Note updated for Amy Bee.", result.getFeedbackToUser());
        assertEquals("Short attention span", AcademicNote.display(
                model.getFilteredStudentList().getFirst().getStudentFields()));
    }

    @Test
    public void execute_caseOnlyDifference_isAnUpdateBecauseNotesAreComparedExactly() throws Exception {
        ModelManager model = modelWithOneStudent();
        new NoteCommand(INDEX_FIRST_STUDENT, CALCULUS).execute(model);

        CommandResult result = new NoteCommand(INDEX_FIRST_STUDENT, new AcademicNote("weak in calculus"))
                .execute(model);

        assertEquals("Note updated for Amy Bee.", result.getFeedbackToUser());
    }

    @Test
    public void execute_sameNote_isNoChangeAndLeavesTheModelAlone() throws Exception {
        ModelManager model = modelWithOneStudent();
        new NoteCommand(INDEX_FIRST_STUDENT, CALCULUS).execute(model);
        Student before = model.getFilteredStudentList().getFirst();
        model.selectStudent(null);

        CommandResult result = new NoteCommand(INDEX_FIRST_STUDENT, new AcademicNote("  Weak   in calculus "))
                .execute(model);

        assertEquals("Note for Amy Bee is already up to date.", result.getFeedbackToUser());
        assertTrue(result.getSelectedStudentIndex().isEmpty());
        assertNull(model.getSelectedStudentId());
        assertEquals(before, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void execute_storedNoteThatCannotBeRead_isTreatedAsUnset() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields(Map.of("academicNote", IntNode.valueOf(3)))));

        CommandResult result = new NoteCommand(INDEX_FIRST_STUDENT, CALCULUS).execute(model);

        assertEquals("Note set for Amy Bee.", result.getFeedbackToUser());
    }

    @Test
    public void execute_keepsEveryOtherStoredField() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build().withStudentFields(new StudentFields(
                Map.of("subject", TextNode.valueOf("Maths"), "futureField", TextNode.valueOf("keep me")))));
        Student before = model.getFilteredStudentList().getFirst();

        new NoteCommand(INDEX_FIRST_STUDENT, CALCULUS).execute(model);

        Student after = model.getFilteredStudentList().getFirst();
        assertEquals(before.getId(), after.getId());
        assertEquals(before.getStudentFields().toStorage().get("subject"),
                after.getStudentFields().toStorage().get("subject"));
        assertEquals(TextNode.valueOf("keep me"), after.getStudentFields().toStorage().get("futureField"));
    }

    @Test
    public void execute_missingStudent_failsWithoutChangingTheModel() {
        ModelManager model = modelWithOneStudent();
        Student before = model.getFilteredStudentList().getFirst();
        NoteCommand command = new NoteCommand(INDEX_SECOND_STUDENT, CALCULUS);

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No student exists at index 2.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
        assertTrue(model.getFilteredStudentList().getFirst().getStudentFields().get(AcademicNote.FIELD).isEmpty());
    }

    @Test
    public void equalsHashCodeAndToString() {
        NoteCommand command = new NoteCommand(INDEX_FIRST_STUDENT, CALCULUS);
        assertEquals(command, new NoteCommand(INDEX_FIRST_STUDENT, new AcademicNote("Weak  in calculus")));
        assertEquals(command.hashCode(),
                new NoteCommand(INDEX_FIRST_STUDENT, new AcademicNote("Weak in calculus")).hashCode());
        assertEquals(command, command);
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new NoteCommand(INDEX_SECOND_STUDENT, CALCULUS)));
        assertFalse(command.equals(new NoteCommand(INDEX_FIRST_STUDENT, new AcademicNote("Other"))));
        assertEquals(NoteCommand.class.getCanonicalName() + "{index=" + Index.fromOneBased(1)
                + ", note=Weak in calculus}", command.toString());
    }
}
