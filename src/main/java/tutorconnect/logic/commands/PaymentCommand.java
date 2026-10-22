package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.time.YearMonth;
import java.util.Objects;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;
import tutorconnect.model.student.Student;

/** Sets or replaces one student's manually recorded status for a month. */
public class PaymentCommand extends Command {
    public static final String COMMAND_WORD = "payment";
    public static final String MESSAGE_SET = "Payment status set for %s (%s): %s.";
    public static final String MESSAGE_UPDATED = "Payment status updated for %s (%s): %s -> %s.";
    public static final String MESSAGE_UNCHANGED = "Payment status for %s (%s) is already %s.";

    private final Index index;
    private final YearMonth month;
    private final PaymentStatus status;

    /** Creates an update targeting a student in the current displayed list. */
    public PaymentCommand(Index index, YearMonth month, PaymentStatus status) {
        this.index = requireNonNull(index);
        this.month = requireNonNull(month);
        this.status = requireNonNull(status);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Student target = VisibleIndexResolver.resolveStudent(index, model);
        PaymentHistory previousHistory;
        try {
            previousHistory = target.getStudentFields().get(PaymentHistory.FIELD).orElseGet(PaymentHistory::empty);
        } catch (IllegalArgumentException exception) {
            throw new CommandException(String.format(PaymentHistory.MESSAGE_UNREADABLE, target.getName()), exception);
        }
        PaymentStatus previous = previousHistory.getStatus(month).orElse(null);
        String identity = target.getName() + " (" + target.getPhone() + ")";
        String monthText = month.toString();
        if (previous == status) {
            return new CommandResult(String.format(MESSAGE_UNCHANGED, identity, monthText, status));
        }

        PaymentHistory updatedHistory = previousHistory.withStatus(month, status);
        Student updatedStudent = target.withStudentFields(
                target.getStudentFields().with(PaymentHistory.FIELD, updatedHistory));
        model.setStudent(target, updatedStudent);
        model.selectStudent(updatedStudent.getId());
        String message = previous == null
                ? String.format(MESSAGE_SET, identity, monthText, status)
                : String.format(MESSAGE_UPDATED, identity, monthText, previous, status);
        return new CommandResult(message);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof PaymentCommand command
                && index.equals(command.index) && month.equals(command.month) && status == command.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, month, status);
    }
}
