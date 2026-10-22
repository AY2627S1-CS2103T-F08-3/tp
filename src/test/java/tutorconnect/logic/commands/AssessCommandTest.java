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
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.AssessmentRecord;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.StudentBuilder;

public class AssessCommandTest {

    private static final AssessmentRecord MIDTERM = AssessmentRecord.of("2026-10-01", "Math Midterm", "85/100");

    private static ModelManager modelWithOneStudent() {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build());
        return model;
    }

    private static AssessmentHistory historyOf(ModelManager model) {
        return AssessmentHistory.of(model.getFilteredStudentList().getFirst().getStudentFields());
    }

    @Test
    public void execute_firstRecord_isAddedAndTheStudentSelected() throws Exception {
        ModelManager model = modelWithOneStudent();

        CommandResult result = new AssessCommand(INDEX_FIRST_STUDENT, MIDTERM).execute(model);

        assertEquals("Assessment added for Amy Bee: #1 2026-10-01 Math Midterm: 85/100.", result.getFeedbackToUser());
        assertEquals(Optional.of(1), result.getSelectedStudentIndex());
        assertEquals(1, historyOf(model).size());
        assertEquals(MIDTERM, historyOf(model).get(0));
        assertEquals(model.getFilteredStudentList().getFirst().getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_messageShowsTheNumberTheRecordGetsInDateOrder() throws Exception {
        ModelManager model = modelWithOneStudent();
        new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-10-05", "B", "1")).execute(model);

        assertEquals("Assessment added for Amy Bee: #1 2026-09-01 A: 2.", new AssessCommand(INDEX_FIRST_STUDENT,
                AssessmentRecord.of("2026-09-01", "A", "2")).execute(model).getFeedbackToUser());
        assertEquals("Assessment added for Amy Bee: #3 2026-10-05 C: 3.", new AssessCommand(INDEX_FIRST_STUDENT,
                AssessmentRecord.of("2026-10-05", "C", "3")).execute(model).getFeedbackToUser());
        assertEquals("Assessment added for Amy Bee: #2 2026-09-15 D: 4.", new AssessCommand(INDEX_FIRST_STUDENT,
                AssessmentRecord.of("2026-09-15", "D", "4")).execute(model).getFeedbackToUser());
        assertEquals("A", historyOf(model).get(0).getTitle());
        assertEquals("D", historyOf(model).get(1).getTitle());
        assertEquals("B", historyOf(model).get(2).getTitle());
        assertEquals("C", historyOf(model).get(3).getTitle());
    }

    @Test
    public void execute_duplicateAssessment_isRejectedWhateverTheGradeAndChangesNothing() throws Exception {
        ModelManager model = modelWithOneStudent();
        new AssessCommand(INDEX_FIRST_STUDENT, MIDTERM).execute(model);
        Student before = model.getFilteredStudentList().getFirst();
        model.selectStudent(null);
        AssessCommand duplicate = new AssessCommand(INDEX_FIRST_STUDENT,
                AssessmentRecord.of("2026-10-01", "  math   MIDTERM ", "A1"));

        CommandException failure = assertThrows(CommandException.class, () -> duplicate.execute(model));

        assertEquals("Error: Amy Bee already has an assessment \"Math Midterm\" on 2026-10-01. "
                + "Remove it first to change it.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
        assertEquals(null, model.getSelectedStudentId());
    }

    @Test
    public void execute_sameTitleOnAnotherDateOrAnotherTitleOnTheSameDate_isAllowed() throws Exception {
        ModelManager model = modelWithOneStudent();
        new AssessCommand(INDEX_FIRST_STUDENT, MIDTERM).execute(model);

        new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-10-02", "Math Midterm", "A")).execute(model);
        new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-10-01", "Math Quiz", "A")).execute(model);

        assertEquals(3, historyOf(model).size());
    }

    @Test
    public void execute_aFullHistory_rejectsAnotherRecord() throws Exception {
        ModelManager model = modelWithOneStudent();
        for (int i = 1; i <= AssessmentHistory.MAX_RECORDS; i++) {
            new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of("2026-01-01", "Test " + i, "A")).execute(model);
        }
        Student before = model.getFilteredStudentList().getFirst();
        AssessCommand overflow = new AssessCommand(INDEX_FIRST_STUDENT,
                AssessmentRecord.of("2026-02-01", "One more", "A"));

        CommandException failure = assertThrows(CommandException.class, () -> overflow.execute(model));

        assertEquals("Error: A student can have at most 100 assessment records.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
        assertEquals(100, historyOf(model).size());
    }

    @Test
    public void execute_unreadableStoredHistory_isReportedAndNeverOverwritten() {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields(Map.of("assessments", TextNode.valueOf("junk")))));
        Student before = model.getFilteredStudentList().getFirst();
        AssessCommand command = new AssessCommand(INDEX_FIRST_STUDENT, MIDTERM);

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: The stored assessment history for Amy Bee could not be read. "
                + "Correct or remove the \"assessments\" entry in the data file.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void execute_keepsEveryOtherStoredField() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build().withStudentFields(new StudentFields(
                Map.of("subject", TextNode.valueOf("Maths"), "futureField", TextNode.valueOf("keep me")))));
        Student before = model.getFilteredStudentList().getFirst();

        new AssessCommand(INDEX_FIRST_STUDENT, MIDTERM).execute(model);

        Student after = model.getFilteredStudentList().getFirst();
        assertEquals(before.getId(), after.getId());
        assertEquals(TextNode.valueOf("Maths"), after.getStudentFields().toStorage().get("subject"));
        assertEquals(TextNode.valueOf("keep me"), after.getStudentFields().toStorage().get("futureField"));
    }

    @Test
    public void execute_missingStudent_failsWithoutChangingTheModel() {
        ModelManager model = modelWithOneStudent();
        Student before = model.getFilteredStudentList().getFirst();
        AssessCommand command = new AssessCommand(INDEX_SECOND_STUDENT, MIDTERM);

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No student exists at index 2.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void equalsHashCodeAndToString() {
        AssessCommand command = new AssessCommand(INDEX_FIRST_STUDENT, MIDTERM);
        assertEquals(command, new AssessCommand(Index.fromOneBased(1),
                AssessmentRecord.of("2026-10-01", "Math  Midterm", "85/100")));
        assertEquals(command.hashCode(), new AssessCommand(Index.fromOneBased(1), MIDTERM).hashCode());
        assertEquals(command, command);
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new AssessCommand(INDEX_SECOND_STUDENT, MIDTERM)));
        assertFalse(command.equals(new AssessCommand(INDEX_FIRST_STUDENT,
                AssessmentRecord.of("2026-10-01", "Math Midterm", "86/100"))));
        assertTrue(command.toString().startsWith(AssessCommand.class.getCanonicalName() + "{index="));
        assertTrue(command.toString().contains("record=2026-10-01 Math Midterm: 85/100}"));
    }
}
