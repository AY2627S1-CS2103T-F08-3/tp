package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.person.Subject;

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
        Person target = VisibleIndexResolver.resolvePerson(index, model);
        Optional<Subject> previous = currentSubject(target);
        String name = target.getName().toString();
        if (previous.isPresent() && previous.get().equals(subject)) {
            // Case-only or spacing-only differences are no change; the stored display value is kept.
            return new CommandResult(String.format(MESSAGE_NO_CHANGE, name, previous.get()));
        }

        Person updated = target.withStudentFields(target.getStudentFields().with(Subject.FIELD, subject));
        model.setPerson(target, updated);
        String message = previous.isEmpty()
                ? String.format(MESSAGE_SET_SUCCESS, name, subject)
                : String.format(MESSAGE_UPDATE_SUCCESS, name, previous.get(), subject);
        return new CommandResult(message, index.getOneBased());
    }

    /** A hand-edited, unreadable stored subject counts as unset rather than crashing the command. */
    private static Optional<Subject> currentSubject(Person student) {
        try {
            return student.getStudentFields().get(Subject.FIELD);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
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
