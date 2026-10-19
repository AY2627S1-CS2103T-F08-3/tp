package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.EducationLevel;

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
        return StudentFieldUpdate.execute(model, index, EducationLevel.FIELD, level,
                MESSAGE_SET_SUCCESS, MESSAGE_UPDATE_SUCCESS, MESSAGE_NO_CHANGE);
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
