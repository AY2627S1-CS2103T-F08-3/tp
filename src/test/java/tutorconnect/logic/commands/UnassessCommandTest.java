package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorconnect.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static tutorconnect.testutil.TypicalIndexes.INDEX_THIRD_STUDENT;

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

public class UnassessCommandTest {

    private static ModelManager modelWithThreeRecords() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build().withStudentFields(new StudentFields(
                Map.of("subject", TextNode.valueOf("Maths"), "futureField", TextNode.valueOf("keep me")))));
        for (String[] record : new String[][] {{"2026-09-01", "A", "1"}, {"2026-10-01", "B", "2"},
            {"2026-11-01", "C", "3"}}) {
            new AssessCommand(INDEX_FIRST_STUDENT, AssessmentRecord.of(record[0], record[1], record[2]))
                    .execute(model);
        }
        return model;
    }

    private static AssessmentHistory historyOf(ModelManager model) {
        return AssessmentHistory.of(model.getFilteredStudentList().getFirst().getStudentFields());
    }

    @Test
    public void execute_removesTheNumberedRecordAndKeepsTheOthersInOrder() throws Exception {
        ModelManager model = modelWithThreeRecords();

        CommandResult result = new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_SECOND_STUDENT).execute(model);

        assertEquals("Assessment removed from Amy Bee: 2026-10-01 B: 2.", result.getFeedbackToUser());
        assertEquals(Optional.of(1), result.getSelectedStudentIndex());
        assertEquals(2, historyOf(model).size());
        assertEquals("A", historyOf(model).get(0).getTitle());
        assertEquals("C", historyOf(model).get(1).getTitle());
        assertEquals(model.getFilteredStudentList().getFirst().getId(), model.getSelectedStudentId());
    }

    @Test
    public void execute_numbersShiftAfterARemoval() throws Exception {
        ModelManager model = modelWithThreeRecords();

        new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_FIRST_STUDENT).execute(model);
        CommandResult result = new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_FIRST_STUDENT).execute(model);

        assertEquals("Assessment removed from Amy Bee: 2026-10-01 B: 2.", result.getFeedbackToUser());
        assertEquals("C", historyOf(model).get(0).getTitle());
    }

    @Test
    public void execute_theLastRecordRemovesTheKeyAndKeepsOtherFields() throws Exception {
        ModelManager model = modelWithThreeRecords();
        Student before = model.getFilteredStudentList().getFirst();
        for (int i = 0; i < 3; i++) {
            new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_FIRST_STUDENT).execute(model);
        }

        Student after = model.getFilteredStudentList().getFirst();
        assertFalse(after.getStudentFields().toStorage().containsKey("assessments"));
        assertEquals(before.getId(), after.getId());
        assertEquals(TextNode.valueOf("Maths"), after.getStudentFields().toStorage().get("subject"));
        assertEquals(TextNode.valueOf("keep me"), after.getStudentFields().toStorage().get("futureField"));
    }

    @Test
    public void execute_aRecordNumberThatDoesNotExist_failsWithoutChangingTheModel() throws Exception {
        ModelManager model = modelWithThreeRecords();
        Student before = model.getFilteredStudentList().getFirst();
        model.selectStudent(null);
        UnassessCommand command = new UnassessCommand(INDEX_FIRST_STUDENT, Index.fromOneBased(4));

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No assessment record 4 for Amy Bee.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
        assertEquals(null, model.getSelectedStudentId());
    }

    @Test
    public void execute_anOversizedRecordNumber_isReportedSafely() throws Exception {
        ModelManager model = modelWithThreeRecords();
        String huge = "9".repeat(100);
        UnassessCommand command = new UnassessCommand(INDEX_FIRST_STUDENT, Index.fromOneBased(huge));

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No assessment record " + huge + " for Amy Bee.", failure.getMessage());
    }

    @Test
    public void execute_aStudentWithNoRecords_hasNothingToRemove() {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build());
        UnassessCommand command = new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_FIRST_STUDENT);

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No assessment record 1 for Amy Bee.", failure.getMessage());
    }

    @Test
    public void execute_unreadableStoredHistory_isReportedAndNeverOverwritten() {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Amy Bee").build()
                .withStudentFields(new StudentFields(Map.of("assessments", TextNode.valueOf("junk")))));
        Student before = model.getFilteredStudentList().getFirst();
        UnassessCommand command = new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_FIRST_STUDENT);

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: The stored assessment history for Amy Bee could not be read. "
                + "Correct or remove the \"assessments\" entry in the data file.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void execute_missingStudent_failsWithoutChangingTheModel() throws Exception {
        ModelManager model = modelWithThreeRecords();
        Student before = model.getFilteredStudentList().getFirst();
        UnassessCommand command = new UnassessCommand(INDEX_SECOND_STUDENT, INDEX_FIRST_STUDENT);

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No student exists at index 2.", failure.getMessage());
        assertEquals(before, model.getFilteredStudentList().getFirst());
        assertEquals(3, historyOf(model).size());
    }

    @Test
    public void equalsHashCodeAndToString() {
        UnassessCommand command = new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_SECOND_STUDENT);
        assertEquals(command, new UnassessCommand(Index.fromOneBased(1), Index.fromOneBased(2)));
        assertEquals(command.hashCode(), new UnassessCommand(Index.fromOneBased(1), Index.fromOneBased(2)).hashCode());
        assertEquals(command, command);
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new UnassessCommand(INDEX_SECOND_STUDENT, INDEX_SECOND_STUDENT)));
        assertFalse(command.equals(new UnassessCommand(INDEX_FIRST_STUDENT, INDEX_THIRD_STUDENT)));
        assertTrue(command.toString().startsWith(UnassessCommand.class.getCanonicalName() + "{index="));
        assertTrue(command.toString().contains(", record="));
    }
}
