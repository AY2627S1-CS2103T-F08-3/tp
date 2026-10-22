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

public class ClearNoteCommandTest {

    private static ModelManager modelWithANote() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build().withStudentFields(new StudentFields(
                Map.of("subject", TextNode.valueOf("Maths"), "futureField", TextNode.valueOf("keep me")))));
        model.addStudent(new StudentBuilder().withName("Other Student").withPhone("91234567").build());
        new NoteCommand(INDEX_FIRST_STUDENT, new AcademicNote("Weak in calculus")).execute(model);
        return model;
    }

    @Test
    public void execute_existingNote_clearsOnlyTheNoteAndSelectsStudent() throws Exception {
        ModelManager model = modelWithANote();
        Student before = model.getFilteredStudentList().getFirst();
        Student other = model.getFilteredStudentList().getLast();
        model.selectStudent(null);

        CommandResult result = new ClearNoteCommand(INDEX_FIRST_STUDENT).execute(model);

        assertEquals("Note cleared for Amy Bee.", result.getFeedbackToUser());
        assertEquals(Optional.of(1), result.getSelectedStudentIndex());
        Student after = model.getFilteredStudentList().getFirst();
        assertEquals(before.getId(), after.getId());
        assertTrue(after.getStudentFields().get(AcademicNote.FIELD).isEmpty());
        assertFalse(after.getStudentFields().toStorage().containsKey("academicNote"));
        assertEquals(TextNode.valueOf("Maths"), after.getStudentFields().toStorage().get("subject"));
        assertEquals(TextNode.valueOf("keep me"), after.getStudentFields().toStorage().get("futureField"));
        assertEquals(after.getId(), model.getSelectedStudentId());
        assertEquals(other, model.getFilteredStudentList().getLast());
    }

    @Test
    public void execute_noNote_isNothingToClearAndLeavesTheModelAlone() throws Exception {
        ModelManager model = modelWithANote();
        new ClearNoteCommand(INDEX_FIRST_STUDENT).execute(model);
        Student before = model.getFilteredStudentList().getFirst();
        model.selectStudent(null);

        CommandResult result = new ClearNoteCommand(INDEX_FIRST_STUDENT).execute(model);

        assertEquals("Amy Bee has no note to clear.", result.getFeedbackToUser());
        assertTrue(result.getSelectedStudentIndex().isEmpty());
        assertNull(model.getSelectedStudentId());
        assertEquals(before, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void execute_storedNoteThatCannotBeRead_isAlsoCleared() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields(Map.of("academicNote", IntNode.valueOf(3)))));

        CommandResult result = new ClearNoteCommand(INDEX_FIRST_STUDENT).execute(model);

        assertEquals("Note cleared for Amy Bee.", result.getFeedbackToUser());
        assertTrue(model.getFilteredStudentList().getFirst().getStudentFields().toStorage().isEmpty());
    }

    @Test
    public void execute_missingStudent_failsWithoutChangingTheModel() throws Exception {
        ModelManager model = modelWithANote();
        model.selectStudent(null);
        Student before = model.getFilteredStudentList().getLast();
        ClearNoteCommand command = new ClearNoteCommand(Index.fromOneBased(3));

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No student exists at index 3.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getLast());
        assertEquals("Weak in calculus", AcademicNote.display(
                model.getFilteredStudentList().getFirst().getStudentFields()));
        assertNull(model.getSelectedStudentId());
    }

    @Test
    public void equalsHashCodeAndToString() {
        ClearNoteCommand command = new ClearNoteCommand(INDEX_FIRST_STUDENT);
        assertEquals(command, new ClearNoteCommand(Index.fromOneBased(1)));
        assertEquals(command.hashCode(), new ClearNoteCommand(Index.fromOneBased(1)).hashCode());
        assertEquals(command, command);
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new ClearNoteCommand(INDEX_SECOND_STUDENT)));
        assertEquals(ClearNoteCommand.class.getCanonicalName() + "{index=" + Index.fromOneBased(1) + "}",
                command.toString());
    }
}
