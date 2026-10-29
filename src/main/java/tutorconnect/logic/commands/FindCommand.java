package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.Messages;
import tutorconnect.model.Model;
import tutorconnect.model.student.NameContainsKeywordsPredicate;

/**
 * Finds and lists all students in the student directory whose name contains any of the argument keywords.
 * Keyword matching is case insensitive.
 */
public class FindCommand extends Command {

    public static final String COMMAND_WORD = "find";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds all students whose names contain any of "
            + "the specified partial names (NFKC-normalized, case-insensitive, OR) in the full register.\n"
            + "Parameters: KEYWORD [MORE_KEYWORDS]...\n"
            + "Example: " + COMMAND_WORD + " alice bob charlie";

    private static final Logger logger = LogsCenter.getLogger(FindCommand.class);

    private final NameContainsKeywordsPredicate predicate;

    public FindCommand(NameContainsKeywordsPredicate predicate) {
        this.predicate = requireNonNull(predicate);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(predicate);
        logger.fine(() -> "Name query applied to full register; matched "
                + model.getFilteredStudentList().size() + " student(s); selection cleared");
        return new CommandResult(
                String.format(Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW, model.getFilteredStudentList().size()),
                CommandResult.SelectionAction.CLEAR);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof FindCommand otherFindCommand)) {
            return false;
        }

        return predicate.equals(otherFindCommand.predicate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .toString();
    }
}
