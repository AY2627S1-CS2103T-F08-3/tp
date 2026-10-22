package tutorconnect.logic.parser;

import java.time.YearMonth;

import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.PaymentCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;

/** Parses the indexed monthly payment status command. */
public class PaymentCommandParser implements Parser<PaymentCommand> {
    @Override
    public PaymentCommand parse(String args) throws ParseException {
        if (StudentText.hasProhibitedCharacters(args)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        StudentParameters.IndexedParameters parameters = StudentParameters.parseIndexed(args,
                "m/MONTH", "s/STATUS");
        VisibleIndex index = VisibleIndex.parse(StudentText.normalize(parameters.index()));
        YearMonth month;
        PaymentStatus status;
        try {
            month = PaymentHistory.parseMonth(StudentText.normalize(parameters.values().get("m/")));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
        try {
            status = PaymentStatus.parse(StudentText.normalize(parameters.values().get("s/")));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
        return new PaymentCommand(index, month, status);
    }
}
