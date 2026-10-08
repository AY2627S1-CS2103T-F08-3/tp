package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Person;

/** Sets the education level of a student selected by displayed index. */
public class LevelCommand extends Command {

    public static final String COMMAND_WORD = "level";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Sets a student's education level.\n"
            + "Parameters: INDEX l/LEVEL\n"
            + "Example: " + COMMAND_WORD + " 1 l/Secondary 4";

    public static final String MESSAGE_SET_SUCCESS = "Education level set for %1$s: %2$s.";
    public static final String MESSAGE_UPDATE_SUCCESS = "Education level updated for %1$s: %2$s -> %3$s.";
    public static final String MESSAGE_NO_CHANGE = "Education level for %1$s is already %2$s.";

    private final Index index;
    private final EducationLevel level;

    /** Creates a command that sets the level of the student shown at {@code index}. */
    public LevelCommand(Index index, EducationLevel level) {
        requireNonNull(index);
        requireNonNull(level);
        this.index = index;
        this.level = level;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Person target = VisibleIndexResolver.resolvePerson(index, model);
        Optional<EducationLevel> previous = currentLevel(target);
        String name = target.getName().toString();
        if (previous.isPresent() && previous.get().equals(level)) {
            return new CommandResult(String.format(MESSAGE_NO_CHANGE, name, previous.get()));
        }

        Person updated = target.withStudentFields(target.getStudentFields().with(EducationLevel.FIELD, level));
        model.setPerson(target, updated);
        String message = previous.isEmpty()
                ? String.format(MESSAGE_SET_SUCCESS, name, level)
                : String.format(MESSAGE_UPDATE_SUCCESS, name, previous.get(), level);
        return new CommandResult(message, index.getOneBased());
    }

    /** A hand-edited, unreadable stored level counts as unset rather than crashing the command. */
    private static Optional<EducationLevel> currentLevel(Person student) {
        try {
            return student.getStudentFields().get(EducationLevel.FIELD);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof LevelCommand otherCommand
                && index.equals(otherCommand.index) && level.equals(otherCommand.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, level);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("level", level).toString();
    }
}
