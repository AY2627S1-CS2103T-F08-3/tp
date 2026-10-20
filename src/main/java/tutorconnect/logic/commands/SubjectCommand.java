package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.Subject;

/** Sets the single subject of a student selected by displayed index. */
public class SubjectCommand extends Command {

    public static final String COMMAND_WORD = "subject";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Sets a student's subject.\n"
            + "Parameters: INDEX s/SUBJECT\n"
            + "Example: " + COMMAND_WORD + " 1 s/O-Level Chemistry";

    public static final String MESSAGE_SET_SUCCESS = "Subject set for %1$s: %2$s.";
    public static final String MESSAGE_UPDATE_SUCCESS = "Subject updated for %1$s: %2$s -> %3$s.";
    public static final String MESSAGE_NO_CHANGE = "Subject for %1$s is already %2$s.";

    private final Index index;
    private final Subject subject;

    /** Creates a command that sets the subject of the student shown at {@code index}. */
    public SubjectCommand(Index index, Subject subject) {
        requireNonNull(index);
        requireNonNull(subject);
        this.index = index;
        this.subject = subject;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        return StudentFieldUpdate.execute(model, index, Subject.FIELD, subject,
                MESSAGE_SET_SUCCESS, MESSAGE_UPDATE_SUCCESS, MESSAGE_NO_CHANGE);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof SubjectCommand otherCommand
                && index.equals(otherCommand.index) && subject.equals(otherCommand.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, subject);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("subject", subject).toString();
    }
}
