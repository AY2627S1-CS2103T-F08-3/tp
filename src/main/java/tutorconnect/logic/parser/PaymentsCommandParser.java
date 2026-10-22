package tutorconnect.logic.parser;

import java.time.YearMonth;
import java.util.Optional;

import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.PaymentsCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;

/** Parses {@code payments m/YYYY-MM [s/pending|completed]}. */
public class PaymentsCommandParser implements Parser<PaymentsCommand> {
    private static final String MESSAGE_MISSING_MONTH = "Error: Missing required parameter: m/MONTH.";

    @Override
    public PaymentsCommand parse(String arguments) throws ParseException {
        if (StudentText.hasProhibitedCharacters(arguments)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        var values = StudentParameters.parseOptional(arguments, "m/MONTH", "s/STATUS");
        String rawMonth = values.get("m/");
        if (rawMonth == null) {
            throw new ParseException(MESSAGE_MISSING_MONTH);
        }
        YearMonth month;
        try {
            month = PaymentHistory.parseMonth(StudentText.normalize(rawMonth));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }

        Optional<PaymentStatus> status = Optional.empty();
        String rawStatus = values.get("s/");
        if (rawStatus != null) {
            try {
                status = Optional.of(PaymentStatus.parse(StudentText.normalize(rawStatus)));
            } catch (IllegalArgumentException exception) {
                throw new ParseException(exception.getMessage(), exception);
            }
        }
        return new PaymentsCommand(month, status);
    }
}
