package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.AcademicNote;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;

/** Explicitly clears the note of a student selected by displayed index. */
public class ClearNoteCommand extends Command {

    public static final String COMMAND_WORD = "clearnote";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Clears a student's note.\n"
            + "Parameters: INDEX\n"
            + "Example: " + COMMAND_WORD + " 1";

    public static final String MESSAGE_CLEARED = "Note cleared for %1$s.";
    public static final String MESSAGE_NOTHING_TO_CLEAR = "%1$s has no note to clear.";

    private final Index index;

    /** Creates a command that clears the note of the student shown at {@code index}. */
    public ClearNoteCommand(Index index) {
        requireNonNull(index);
        this.index = index;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Student target = VisibleIndexResolver.resolveStudent(index, model);
        String name = target.getName().toString();
        Map<String, JsonNode> stored = target.getStudentFields().toStorage();
        if (!stored.containsKey(AcademicNote.FIELD.key())) {
            return new CommandResult(String.format(MESSAGE_NOTHING_TO_CLEAR, name));
        }

        // Removing the key also clears a stored value that could not be read.
        stored.remove(AcademicNote.FIELD.key());
        model.setStudent(target, target.withStudentFields(new StudentFields(stored)));
        return new CommandResult(String.format(MESSAGE_CLEARED, name), index.getOneBased());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ClearNoteCommand otherCommand && index.equals(otherCommand.index);
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
