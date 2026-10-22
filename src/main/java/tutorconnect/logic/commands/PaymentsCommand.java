package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;
import tutorconnect.model.student.Student;

/** Lists recorded payment statuses for a calendar month without changing student selection or storage. */
public class PaymentsCommand extends Command {
    public static final String COMMAND_WORD = "payments";
    public static final String MESSAGE_EMPTY = "No recorded payments for %s.";

    private final YearMonth month;
    private final Optional<PaymentStatus> statusFilter;

    /** Creates a month listing, optionally restricted to one status. */
    public PaymentsCommand(YearMonth month, Optional<PaymentStatus> statusFilter) {
        this.month = requireNonNull(month);
        this.statusFilter = requireNonNull(statusFilter);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        List<String> rows = new ArrayList<>();
        for (Student student : model.getStudentDirectory().getStudentList()) {
            Optional<PaymentStatus> recorded;
            try {
                recorded = student.getStudentFields().get(PaymentHistory.FIELD)
                        .flatMap(history -> history.getStatus(month));
            } catch (IllegalArgumentException exception) {
                throw new CommandException(String.format(PaymentHistory.MESSAGE_UNREADABLE, student.getName()),
                        exception);
            }
            if (recorded.isEmpty() || statusFilter.filter(expected -> expected != recorded.get()).isPresent()) {
                continue;
            }
            rows.add(student.getName() + " (" + student.getPhone() + ") — " + month + ": " + recorded.get());
        }
        if (rows.isEmpty()) {
            return new CommandResult(String.format(MESSAGE_EMPTY, month));
        }
        return new CommandResult(String.join("\n", rows));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof PaymentsCommand command
                && month.equals(command.month) && statusFilter.equals(command.statusFilter);
    }

    @Override
    public int hashCode() {
        return 31 * month.hashCode() + statusFilter.hashCode();
    }
}
