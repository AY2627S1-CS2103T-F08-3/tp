package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.HourlyRate;
import tutorconnect.model.student.Student;

/** Sets a student's hourly rate using the rate's displayed index. */
public class RateCommand extends Command implements AtomicCommand {

    public static final String COMMAND_WORD = "rate";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Sets a student's hourly rate in SGD.\n"
            + "Parameters: INDEX r/RATE\n"
            + "Example: " + COMMAND_WORD + " 1 r/80";

    public static final String MESSAGE_SET_SUCCESS = "Hourly rate set for %1$s: S$%2$s.";
    public static final String MESSAGE_UPDATE_SUCCESS = "Hourly rate updated for %1$s: S$%2$s -> S$%3$s.";
    public static final String MESSAGE_NO_CHANGE = "Hourly rate for %1$s is already S$%2$s.";

    private final Index index;
    private final HourlyRate hourlyRate;

    /** Creates a rate update targeting a validated displayed index. */
    public RateCommand(Index index, HourlyRate hourlyRate) {
        requireNonNull(index);
        requireNonNull(hourlyRate);
        this.index = index;
        this.hourlyRate = hourlyRate;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Student target = VisibleIndexResolver.resolveStudent(index, model);
        Optional<HourlyRate> previous = target.getHourlyRate();
        String name = target.getName().toString();
        if (previous.isPresent() && previous.get().equals(hourlyRate)) {
            return new CommandResult(String.format(MESSAGE_NO_CHANGE, name, hourlyRate));
        }

        Student updated = target.withHourlyRate(hourlyRate);
        model.setStudent(target, updated);
        String message = previous.isEmpty()
                ? String.format(MESSAGE_SET_SUCCESS, name, hourlyRate)
                : String.format(MESSAGE_UPDATE_SUCCESS, name, previous.get(), hourlyRate);
        return new CommandResult(message, index.getOneBased());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof RateCommand otherCommand
                && index.equals(otherCommand.index) && hourlyRate.equals(otherCommand.hourlyRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, hourlyRate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("hourlyRate", hourlyRate).toString();
    }
}
