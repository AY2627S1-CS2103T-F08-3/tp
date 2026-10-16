package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.VisibleIndex;
import tutorconnect.model.Model;
import tutorconnect.model.student.Student;

/**
 * Deletes a student identified using its displayed index from the address book.
 */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Deletes the student identified by the index number used in the displayed student list.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";

    public static final String MESSAGE_DELETE_STUDENT_SUCCESS = "Student deleted: %1$s.";

    private final Index targetIndex;

    /** Creates a deletion from an existing bounded index. */
    public DeleteCommand(Index targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
    }

    /** Creates a deletion from a validated visible index. */
    public DeleteCommand(VisibleIndex targetIndex) {
        this(Index.fromOneBased(requireNonNull(targetIndex).toString()));
    }

    /** Resolves the target before preparing a complete proposed state for persistence. */
    public Student resolveTarget(Model model) throws CommandException {
        return VisibleIndexResolver.resolveStudent(targetIndex, model);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Student studentToDelete = resolveTarget(model);
        model.deleteStudent(studentToDelete);
        return new CommandResult(String.format(MESSAGE_DELETE_STUDENT_SUCCESS, studentToDelete.getName()),
                CommandResult.SelectionAction.PRESERVE);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof DeleteCommand otherDeleteCommand)) {
            return false;
        }

        return targetIndex.equals(otherDeleteCommand.targetIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex.getOneBasedString())
                .toString();
    }
}
