package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.AcademicNote;

/** Sets or replaces the single freeform note of a student selected by displayed index. */
public class NoteCommand extends Command {

    public static final String COMMAND_WORD = "note";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Sets or replaces a student's note.\n"
            + "Parameters: INDEX m/NOTE\n"
            + "Example: " + COMMAND_WORD + " 1 m/Weak in calculus; needs more practice";

    public static final String MESSAGE_SET_SUCCESS = "Note set for %1$s.";
    public static final String MESSAGE_UPDATE_SUCCESS = "Note updated for %1$s.";
    public static final String MESSAGE_NO_CHANGE = "Note for %1$s is already up to date.";

    private final Index index;
    private final AcademicNote note;

    /** Creates a command that sets the note of the student shown at {@code index}. */
    public NoteCommand(Index index, AcademicNote note) {
        requireNonNull(index);
        requireNonNull(note);
        this.index = index;
        this.note = note;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        return StudentFieldUpdate.execute(model, index, AcademicNote.FIELD, note,
                MESSAGE_SET_SUCCESS, MESSAGE_UPDATE_SUCCESS, MESSAGE_NO_CHANGE);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof NoteCommand otherCommand
                && index.equals(otherCommand.index) && note.equals(otherCommand.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index.getOneBasedString(), note);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("note", note).toString();
    }
}
