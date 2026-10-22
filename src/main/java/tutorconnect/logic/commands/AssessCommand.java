package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.AssessmentRecord;
import tutorconnect.model.student.Student;

/** Adds one dated assessment record to the student selected by displayed index. */
public class AssessCommand extends Command {

    public static final String COMMAND_WORD = "assess";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds an assessment record to a student.\n"
            + "Parameters: INDEX d/DATE t/TITLE g/GRADE\n"
            + "Example: " + COMMAND_WORD + " 1 d/2026-10-01 t/Math Midterm g/85/100";

    public static final String MESSAGE_ADDED = "Assessment added for %1$s: #%2$d %3$s.";
    public static final String MESSAGE_DUPLICATE =
            "Error: %1$s already has an assessment \"%2$s\" on %3$s. Remove it first to change it.";
    public static final String MESSAGE_LIMIT = "Error: A student can have at most "
            + AssessmentHistory.MAX_RECORDS + " assessment records.";

    private final Index index;
    private final AssessmentRecord record;

    /** Creates a command that adds {@code record} to the student shown at {@code index}. */
    public AssessCommand(Index index, AssessmentRecord record) {
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

        Optional<AssessmentRecord> existing = history.findSameAssessment(record);
        if (existing.isPresent()) {
            throw new CommandException(String.format(MESSAGE_DUPLICATE, name, existing.get().getTitle(),
                    record.getDate()));
        }
        if (history.isFull()) {
            throw new CommandException(MESSAGE_LIMIT);
        }

        int number = history.numberIfAdded(record);
        model.setStudent(target, AssessmentUpdate.withHistory(target, history.with(record)));
        return new CommandResult(String.format(MESSAGE_ADDED, name, number, record), index.getOneBased());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof AssessCommand otherCommand
                && index.equals(otherCommand.index) && record.equals(otherCommand.record);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index.getOneBasedString(), record);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("record", record).toString();
    }
}
