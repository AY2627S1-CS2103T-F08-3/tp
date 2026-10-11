package tutorconnect.logic.commands;

import static tutorconnect.logic.commands.CommandTestUtil.assertCommandSuccess;
import static tutorconnect.logic.commands.CommandTestUtil.showStudentAtIndex;
import static tutorconnect.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorconnect.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;
import tutorconnect.model.UserPrefs;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListCommand.
 */
public class ListCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        assertCommandSuccess(new ListCommand(), model, new CommandResult(String.format(ListCommand.MESSAGE_SUCCESS, 7),
                CommandResult.SelectionAction.CLEAR), expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);
        assertCommandSuccess(new ListCommand(), model, new CommandResult(String.format(ListCommand.MESSAGE_SUCCESS, 7),
                CommandResult.SelectionAction.CLEAR), expectedModel);
    }
}
