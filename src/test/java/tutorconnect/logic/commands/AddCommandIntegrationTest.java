package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorconnect.logic.commands.CommandTestUtil.assertCommandFailure;
import static tutorconnect.logic.commands.CommandTestUtil.assertCommandSuccess;
import static tutorconnect.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Student;
import tutorconnect.testutil.StudentBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code AddCommand}.
 */
public class AddCommandIntegrationTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
    }

    @Test
    public void execute_newStudent_success() {
        Student validStudent = new StudentBuilder().build();

        Model expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        expectedModel.addStudent(validStudent);

        assertCommandSuccess(new AddCommand(validStudent), model,
                String.format(AddCommand.MESSAGE_SUCCESS, validStudent.getName()),
                expectedModel);
    }

    @Test
    public void execute_duplicateStudent_throwsCommandException() {
        Student studentInList = model.getStudentDirectory().getStudentList().get(0);
        assertCommandFailure(new AddCommand(studentInList), model,
                AddCommand.MESSAGE_DUPLICATE_STUDENT);
    }

    @Test
    public void execute_normalizedDuplicate_rejectedEvenWhenHidden() {
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        model.updateFilteredStudentList(student -> false);
        Student duplicate = new StudentBuilder(original).withName("  ALICE   PAULINE  ")
                .withPhone("+65-9435-1253").withAddress("Another address").build();
        assertCommandFailure(new AddCommand(duplicate), model, AddCommand.MESSAGE_DUPLICATE_STUDENT);
    }

    @Test
    public void execute_sameNameDifferentPhone_accepted() throws Exception {
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        Student added = new StudentBuilder(original).withPhone("61234567").build();
        assertEquals("Student added: Alice Pauline.", new AddCommand(added).execute(model).getFeedbackToUser());
        assertEquals(added, model.getStudentDirectory().getStudentList().getLast());
    }

    @Test
    public void execute_sharedPhoneDifferentName_warningAppended() throws Exception {
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        model.updateFilteredStudentList(student -> false);
        Student added = new StudentBuilder(original).withName("Another Student").build();
        assertEquals("Student added: Another Student. Warning: Another student uses this phone number.",
                new AddCommand(added).execute(model).getFeedbackToUser());
        assertEquals(added, model.getStudentDirectory().getStudentList().getLast());
    }

}
