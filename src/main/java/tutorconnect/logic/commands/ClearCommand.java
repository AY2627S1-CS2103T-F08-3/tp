package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorconnect.model.Model;
import tutorconnect.model.StudentDirectory;

/**
 * Removes every student from TutorConnect.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_SUCCESS = "All students have been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setStudentDirectory(new StudentDirectory());
        model.selectStudent(null);
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
