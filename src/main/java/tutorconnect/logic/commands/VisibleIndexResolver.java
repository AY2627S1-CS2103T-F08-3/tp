package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.Messages;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.Student;

/** Resolves a user-visible one-based index against the model's currently displayed list. */
public final class VisibleIndexResolver {

    private VisibleIndexResolver() {}

    /** Returns the student at {@code index}, or an error if that displayed entry does not exist. */
    public static Student resolveStudent(Index index, Model model) throws CommandException {
        requireNonNull(index);
        requireNonNull(model);

        if (!index.isWithinSize(model.getFilteredStudentList().size())) {
            throw new CommandException(String.format(
                    Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, index.getOneBasedString()));
        }
        return model.getFilteredStudentList().get(index.getZeroBasedExact());
    }
}
