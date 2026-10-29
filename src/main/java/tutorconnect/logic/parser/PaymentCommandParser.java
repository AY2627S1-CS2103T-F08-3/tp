package tutorconnect.logic.parser;

import java.time.YearMonth;

import tutorconnect.commons.core.index.Index;
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
        String rawIndex = StudentText.normalize(parameters.index());
        if (rawIndex.isEmpty()) {
            throw new ParseException(ParserUtil.MESSAGE_MISSING_INDEX);
        }
        if (!rawIndex.matches("[1-9][0-9]*")) {
            throw new ParseException(VisibleIndex.MESSAGE_INVALID);
        }
        Index index = Index.fromOneBased(rawIndex);
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
