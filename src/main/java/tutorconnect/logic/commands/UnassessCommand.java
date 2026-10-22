package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.AssessmentRecord;
import tutorconnect.model.student.Student;

/** Removes one assessment record, by its number in the student's history, so a wrong entry can be corrected. */
public class UnassessCommand extends Command {

    public static final String COMMAND_WORD = "unassess";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Removes an assessment record from a student.\n"
            + "Parameters: INDEX i/RECORD\n"
            + "Example: " + COMMAND_WORD + " 1 i/2";

    public static final String MESSAGE_REMOVED = "Assessment removed from %1$s: %2$s.";
    public static final String MESSAGE_NO_RECORD = "Error: No assessment record %2$s for %1$s.";
    public static final String MESSAGE_INVALID_RECORD =
            "Error: Invalid record number. Enter a positive whole number without leading zeroes.";

    private static final Logger logger = LogsCenter.getLogger(UnassessCommand.class);

    private final Index index;
    private final Index record;

    /** Creates a command that removes record number {@code record} of the student shown at {@code index}. */
    public UnassessCommand(Index index, Index record) {
        requireNonNull(index);
        requireNonNull(record);
        this.index = index;
        this.record = record;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Student target = VisibleIndexResolver.resolveStudent(index, model);
        String name = target.getName().toString();
        AssessmentHistory history = AssessmentUpdate.historyOf(target);

        if (!record.isWithinSize(history.size())) {
            logger.fine("Student " + target.getId() + " has no assessment record " + record.getOneBasedString());
            throw new CommandException(String.format(MESSAGE_NO_RECORD, name, record.getOneBasedString()));
        }
        AssessmentRecord removed = history.get(record.getZeroBasedExact());
        model.setStudent(target, AssessmentUpdate.withHistory(target, history.without(record.getZeroBasedExact())));
        logger.fine("Removed assessment #" + record.getOneBasedString() + " from student " + target.getId());
        return new CommandResult(String.format(MESSAGE_REMOVED, name, removed), index.getOneBased());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof UnassessCommand otherCommand
                && index.equals(otherCommand.index) && record.equals(otherCommand.record);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index.getOneBasedString(), record.getOneBasedString());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("record", record).toString();
    }
}
