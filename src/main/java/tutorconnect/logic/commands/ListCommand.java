package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;
import static tutorconnect.model.Model.PREDICATE_SHOW_ALL_STUDENTS;

import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.model.Model;

/**
 * Lists all students in the student directory to the user.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";

    public static final String MESSAGE_SUCCESS = "Listed %d students.";
    public static final String MESSAGE_EMPTY = "No students found.";
    public static final String MESSAGE_LOAD_FAILURE = "Error: Student list could not be loaded.";

    private static final Logger logger = LogsCenter.getLogger(ListCommand.class);

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);
        int count = model.getFilteredStudentList().size();
        logger.fine(() -> "Full register view restored; " + count + " student(s); selection cleared");
        return new CommandResult(count == 0 ? MESSAGE_EMPTY : String.format(MESSAGE_SUCCESS, count),
                CommandResult.SelectionAction.CLEAR);
    }
}
