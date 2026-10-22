package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.logic.Messages;
import tutorconnect.model.Model;
import tutorconnect.model.student.StudentCriteriaPredicate;

/** Replaces the displayed view with matching students from the full register in stored order. */
public class FilterCommand extends Command {
    public static final String COMMAND_WORD = "filter";
    public static final String MESSAGE_USAGE = "filter [l/LEVEL] [s/SUBJECT]";
    public static final String MESSAGE_MISSING_CRITERIA =
            "Error: Supply at least one filter criterion: l/LEVEL or s/SUBJECT.";

    private static final Logger logger = LogsCenter.getLogger(FilterCommand.class);

    private final StudentCriteriaPredicate predicate;

    public FilterCommand(StudentCriteriaPredicate predicate) {
        this.predicate = requireNonNull(predicate);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(predicate);
        logger.fine(() -> "Criteria query applied to full register; level=" + predicate.level().isPresent()
                + "; subject=" + predicate.subject().isPresent() + "; matched "
                + model.getFilteredStudentList().size() + " student(s); selection cleared");
        return new CommandResult(String.format(Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW,
                model.getFilteredStudentList().size()), CommandResult.SelectionAction.CLEAR);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof FilterCommand command && predicate.equals(command.predicate);
    }
}
