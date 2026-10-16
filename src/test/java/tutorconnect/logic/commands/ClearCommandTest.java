package tutorconnect.logic.commands;

import static tutorconnect.logic.commands.CommandTestUtil.assertCommandSuccess;
import static tutorconnect.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.Test;

import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.UserPrefs;

public class ClearCommandTest {

    @Test
    public void execute_emptyStudentDirectory_success() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager();

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_nonEmptyStudentDirectory_success() {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        expectedModel.setStudentDirectory(new StudentDirectory());

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
    }

}
