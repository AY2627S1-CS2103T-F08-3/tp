package tutorconnect.logic.commands;

import java.util.Optional;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentField;

/** Shared staged update for commands that set one optional student field. */
final class StudentFieldUpdate {
    private StudentFieldUpdate() {}

    /** Retains identity, unrelated fields and the original display value on equivalent input. */
    static <T> CommandResult execute(Model model, Index index, StudentField<T> field, T value,
            String setMessage, String updateMessage, String noChangeMessage) throws CommandException {
        Student target = VisibleIndexResolver.resolveStudent(index, model);
        Optional<T> previous = currentValue(target, field);
        String name = target.getName().toString();
        if (previous.isPresent() && previous.get().equals(value)) {
            return new CommandResult(String.format(noChangeMessage, name, previous.get()));
        }

        model.setStudent(target, target.withStudentFields(target.getStudentFields().with(field, value)));
        String message = previous.isEmpty()
                ? String.format(setMessage, name, value)
                : String.format(updateMessage, name, previous.get(), value);
        if (model.getFilteredStudentList().stream().noneMatch(student -> student.getId().equals(target.getId()))) {
            // A changed level or subject can remove this row from the current discovery view.
            model.selectStudent(null);
            return new CommandResult(message, CommandResult.SelectionAction.CLEAR);
        }
        return new CommandResult(message, index.getOneBased());
    }

    /** Retains level/subject repair behavior for unreadable values in an in-memory snapshot. */
    private static <T> Optional<T> currentValue(Student student, StudentField<T> field) {
        try {
            return student.getStudentFields().get(field);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
