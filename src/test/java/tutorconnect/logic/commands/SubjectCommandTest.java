package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorconnect.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.Subject;
import tutorconnect.testutil.StudentBuilder;

public class SubjectCommandTest {

    private static final Subject H2_MATH = new Subject("H2 Math");

    private static ModelManager modelWithOneStudent() {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build());
        return model;
    }

    @Test
    public void execute_emptySubject_setsSubjectAndSelectsStudent() throws Exception {
        ModelManager model = modelWithOneStudent();

        CommandResult result = new SubjectCommand(INDEX_FIRST_STUDENT, H2_MATH).execute(model);

        assertEquals("Subject set for Amy Bee: H2 Math.", result.getFeedbackToUser());
        assertEquals(Optional.of(1), result.getSelectedStudentIndex());
        Student student = model.getFilteredStudentList().getFirst();
        assertEquals(Optional.of(H2_MATH), student.getStudentFields().get(Subject.FIELD));
        assertEquals(student.getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_differentSubject_replacesSubjectWithoutAppending() throws Exception {
        ModelManager model = modelWithOneStudent();
        new SubjectCommand(INDEX_FIRST_STUDENT, H2_MATH).execute(model);

        CommandResult result = new SubjectCommand(INDEX_FIRST_STUDENT, new Subject("Chemistry")).execute(model);

        assertEquals("Subject updated for Amy Bee: H2 Math -> Chemistry.", result.getFeedbackToUser());
        assertEquals("Chemistry", model.getFilteredStudentList().getFirst().getStudentFields().display("subject"));
    }

    @Test
    public void execute_equivalentSubject_isNoChangeAndKeepsTheStoredDisplayValue() throws Exception {
        ModelManager model = modelWithOneStudent();
        new SubjectCommand(INDEX_FIRST_STUDENT, H2_MATH).execute(model);
        Student before = model.getFilteredStudentList().getFirst();
        model.selectStudent(null);

        CommandResult result = new SubjectCommand(INDEX_FIRST_STUDENT, new Subject("h2   MATH")).execute(model);

        assertEquals("Subject for Amy Bee is already H2 Math.", result.getFeedbackToUser());
        assertTrue(result.getSelectedStudentIndex().isEmpty());
        assertEquals(null, model.getSelectedStudentId());
        assertEquals(before, model.getFilteredStudentList().getFirst());
        assertEquals("H2 Math", model.getFilteredStudentList().getFirst().getStudentFields().display("subject"));
    }

    @Test
    public void execute_storedSubjectThatCannotBeRead_isTreatedAsUnset() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields(Map.of("subject", TextNode.valueOf("@@")))));

        CommandResult result = new SubjectCommand(INDEX_FIRST_STUDENT, H2_MATH).execute(model);

        assertEquals("Subject set for Amy Bee: H2 Math.", result.getFeedbackToUser());
    }

    @Test
    public void execute_missingStudent_failsWithoutChangingTheModel() {
        ModelManager model = modelWithOneStudent();
        Student before = model.getFilteredStudentList().getFirst();

        SubjectCommand command = new SubjectCommand(INDEX_SECOND_STUDENT, H2_MATH);
        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No student exists at index 2.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
        assertTrue(model.getFilteredStudentList().getFirst().getStudentFields().get(Subject.FIELD).isEmpty());
    }

    @Test
    public void equalsHashCodeAndToString() {
        SubjectCommand command = new SubjectCommand(INDEX_FIRST_STUDENT, H2_MATH);
        assertEquals(command, new SubjectCommand(INDEX_FIRST_STUDENT, new Subject("h2 math")));
        assertEquals(command.hashCode(), new SubjectCommand(INDEX_FIRST_STUDENT, new Subject("H2 MATH")).hashCode());
        assertEquals(command, command);
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new SubjectCommand(INDEX_SECOND_STUDENT, H2_MATH)));
        assertFalse(command.equals(new SubjectCommand(INDEX_FIRST_STUDENT, new Subject("H1 Math"))));
        assertEquals(SubjectCommand.class.getCanonicalName() + "{index=" + Index.fromOneBased(1)
                + ", subject=H2 Math}", command.toString());
    }
}
