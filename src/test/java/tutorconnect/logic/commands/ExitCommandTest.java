package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorconnect.logic.commands.CommandTestUtil.assertCommandSuccess;
import static tutorconnect.logic.commands.ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT;

import org.junit.jupiter.api.Test;

import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;

public class ExitCommandTest {
    private Model model = new ModelManager();
    private Model expectedModel = new ModelManager();

    @Test
    public void execute_exit_success() {
        CommandResult expectedCommandResult = new CommandResult(MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true);
        assertCommandSuccess(new ExitCommand(), model, expectedCommandResult, expectedModel);
    }

    @Test
    public void message_namesTheProduct() {
        assertEquals("Exiting TutorConnect as requested ...", MESSAGE_EXIT_ACKNOWLEDGEMENT);
    }
}
