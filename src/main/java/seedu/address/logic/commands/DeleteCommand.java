package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.VisibleIndex;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;

/**
 * Deletes a person identified using its displayed index from the address book.
 */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Deletes the person identified by the index number used in the displayed person list.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";

    public static final String MESSAGE_DELETE_PERSON_SUCCESS = "Student deleted: %1$s.";

    private final VisibleIndex targetIndex;

    /** Creates a deletion from an existing bounded index. */
    public DeleteCommand(Index targetIndex) {
        try {
            this.targetIndex = VisibleIndex.parse(Integer.toString(targetIndex.getOneBased()));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Creates a deletion from a validated visible index. */
    public DeleteCommand(VisibleIndex targetIndex) {
        this.targetIndex = targetIndex;
    }

    /** Resolves the target before preparing a complete proposed state for persistence. */
    public Person resolveTarget(Model model) throws CommandException {
        return targetIndex.resolve(model.getFilteredPersonList());
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        var personToDelete = VisibleIndexResolver.resolvePerson(targetIndex, model);
        model.deletePerson(personToDelete);
        return new CommandResult(String.format(MESSAGE_DELETE_PERSON_SUCCESS, personToDelete.getName()),
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
                .add("targetIndex", targetIndex)
                .toString();
    }
}
