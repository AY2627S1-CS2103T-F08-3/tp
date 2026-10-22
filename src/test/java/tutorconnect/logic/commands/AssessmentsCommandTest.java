package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tutorconnect.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorconnect.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.AssessmentRecord;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.StudentBuilder;

public class AssessmentsCommandTest {

    private static ModelManager modelWithTwoStudents() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build());
        model.addStudent(new StudentBuilder().withName("Other Student").withPhone("91234567").build());
        new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-10-05", "Later test", "B+")).execute(model);
        new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-09-01", "Earlier test", "85/100"))
                .execute(model);
        return model;
    }

    @Test
    public void execute_listsTheRecordsInNumberedDateOrder() throws Exception {
        ModelManager model = modelWithTwoStudents();

        CommandResult result = new AssessmentsCommand(INDEX_FIRST_STUDENT).execute(model);

        assertEquals("Assessments for Amy Bee:\n1. 2026-09-01 Earlier test: 85/100\n2. 2026-10-05 Later test: B+",
                result.getFeedbackToUser());
        assertEquals(Optional.of(1), result.getSelectedStudentIndex());
    }

    @Test
    public void execute_aStudentWithNoRecords_hasAnEmptyMessage() throws Exception {
        ModelManager model = modelWithTwoStudents();

        CommandResult result = new AssessmentsCommand(INDEX_SECOND_STUDENT).execute(model);

        assertEquals("Other Student has no assessment records.", result.getFeedbackToUser());
    }

    @Test
    public void execute_selectsTheStudentButChangesNoData() throws Exception {
        ModelManager model = modelWithTwoStudents();
        ReadOnlyStudentDirectory before = new StudentDirectory(model.getStudentDirectory());
        model.selectStudent(null);

        new AssessmentsCommand(INDEX_SECOND_STUDENT).execute(model);

        assertEquals(before, model.getStudentDirectory());
        assertEquals(model.getFilteredStudentList().getLast().getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_unreadableStoredHistory_isReported() {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields(Map.of("assessments", IntNode.valueOf(5)))));
        model.selectStudent(null);
        AssessmentsCommand command = new AssessmentsCommand(INDEX_FIRST_STUDENT);

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: The stored assessment history for Amy Bee could not be read. "
                + "Correct or remove the \"assessments\" entry in the data file.", failure.getMessage());
        assertNull(model.getSelectedStudentId());
    }

    @Test
    public void execute_missingStudent_failsWithoutChangingTheSelection() throws Exception {
        ModelManager model = modelWithTwoStudents();
        model.selectStudent(null);
        AssessmentsCommand command = new AssessmentsCommand(Index.fromOneBased(3));

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No student exists at index 3.", failure.getMessage());
        assertNull(model.getSelectedStudentId());
    }

    @Test
    public void equalsHashCodeAndToString() {
        AssessmentsCommand command = new AssessmentsCommand(INDEX_FIRST_STUDENT);
        assertEquals(command, new AssessmentsCommand(Index.fromOneBased(1)));
        assertEquals(command.hashCode(), new AssessmentsCommand(Index.fromOneBased(1)).hashCode());
        assertEquals(command, command);
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new AssessmentsCommand(INDEX_SECOND_STUDENT)));
        assertEquals(AssessmentsCommand.class.getCanonicalName() + "{index=" + Index.fromOneBased(1) + "}",
                command.toString());
    }

    @Test
    public void studentsRemainUnchangedInTheModelList() throws Exception {
        ModelManager model = modelWithTwoStudents();
        Student first = model.getFilteredStudentList().getFirst();

        new AssessmentsCommand(INDEX_FIRST_STUDENT).execute(model);

        assertEquals(first, model.getFilteredStudentList().getFirst());
    }
}
