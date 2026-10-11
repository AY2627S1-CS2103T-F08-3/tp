package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorconnect.model.Model;
import tutorconnect.model.StudentDirectory;

/**
 * Clears the address book.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_SUCCESS = "Address book has been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setStudentDirectory(new StudentDirectory());
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
