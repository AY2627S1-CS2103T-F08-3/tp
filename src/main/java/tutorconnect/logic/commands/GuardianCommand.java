package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.GuardianPhone;

/** Sets the guardian phone number of a student selected by displayed index. */
public class GuardianCommand extends Command implements AtomicCommand {

    public static final String COMMAND_WORD = "guardian";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Sets a student's guardian contact.\n"
            + "Parameters: INDEX g/PHONE\n"
            + "Example: " + COMMAND_WORD + " 1 g/91234567";

    public static final String MESSAGE_SET_SUCCESS = "Guardian contact set for %1$s: %2$s.";
    public static final String MESSAGE_UPDATE_SUCCESS = "Guardian contact updated for %1$s: %2$s -> %3$s.";
    public static final String MESSAGE_NO_CHANGE = "Guardian contact for %1$s is already %2$s.";

    private final Index index;
    private final GuardianPhone guardianPhone;

    /** Creates a guardian update targeting a validated displayed index. */
    public GuardianCommand(Index index, GuardianPhone guardianPhone) {
        requireNonNull(index);
        requireNonNull(guardianPhone);
        this.index = index;
        this.guardianPhone = guardianPhone;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        return StudentFieldUpdate.execute(model, index, GuardianPhone.FIELD, guardianPhone,
                MESSAGE_SET_SUCCESS, MESSAGE_UPDATE_SUCCESS, MESSAGE_NO_CHANGE);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof GuardianCommand otherCommand
                && index.equals(otherCommand.index) && guardianPhone.equals(otherCommand.guardianPhone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, guardianPhone);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("guardianPhone", guardianPhone).toString();
    }
}
