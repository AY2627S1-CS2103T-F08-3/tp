package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.Student;

/** Lists the assessment records of the student selected by displayed index. Read-only; never saves. */
public class AssessmentsCommand extends Command {

    public static final String COMMAND_WORD = "assessments";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists a student's assessment records.\n"
            + "Parameters: INDEX\n"
            + "Example: " + COMMAND_WORD + " 1";

    public static final String MESSAGE_HEADER = "Assessments for %1$s:";
    public static final String MESSAGE_EMPTY = "%1$s has no assessment records.";

    private final Index index;

    /** Creates a command that lists the records of the student shown at {@code index}. */
    public AssessmentsCommand(Index index) {
        requireNonNull(index);
        this.index = index;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Student target = VisibleIndexResolver.resolveStudent(index, model);
        String name = target.getName().toString();
        AssessmentHistory history = AssessmentUpdate.historyOf(target);

        model.selectStudent(target.getId());
        if (history.isEmpty()) {
            return new CommandResult(String.format(MESSAGE_EMPTY, name), index.getOneBased());
        }
        StringBuilder message = new StringBuilder(String.format(MESSAGE_HEADER, name));
        for (int i = 0; i < history.size(); i++) {
            message.append('\n').append(i + 1).append(". ").append(history.get(i));
        }
        return new CommandResult(message.toString(), index.getOneBased());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof AssessmentsCommand otherCommand && index.equals(otherCommand.index);
    }

    @Override
    public int hashCode() {
        return index.getOneBasedString().hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).toString();
    }
}
