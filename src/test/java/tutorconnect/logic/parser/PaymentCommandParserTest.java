package tutorconnect.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorconnect.testutil.Assert.assertThrows;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.logic.Messages;
import tutorconnect.logic.commands.PaymentCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;

public class PaymentCommandParserTest {
    private final PaymentCommandParser parser = new PaymentCommandParser();

    @Test
    public void parse_reorderedPrefixesAndNormalizedValues_sameCommand() throws Exception {
        PaymentCommand expected = new PaymentCommand(VisibleIndex.parse("1"), YearMonth.of(2026, 10),
                PaymentStatus.PENDING);
        for (String args : List.of(" 1 m/2026-10 s/pending", " 1 s/pending m/2026-10",
                "  1   m/2026-10   s/pending  ", "\u00a0１ m/２０２６-１０ s/pending")) {
            assertEquals(expected, parser.parse(args));
        }
        assertEquals(expected, new TutorConnectParser().parseCommand("payment 1 s/pending m/2026-10"));
    }

    @Test
    public void parse_reportsStructuralErrorsBeforeValueErrors() {
        assertFailure("1 m/2026-10", "Error: Missing required parameter: s/STATUS.");
        assertFailure("1 s/pending", "Error: Missing required parameter: m/MONTH.");
        assertFailure("1 m/2026-10 s/pending m/2026-11",
                "Error: Parameter m/ may be specified only once.");
        assertFailure("1 s/pending s/completed m/2026-10",
                "Error: Parameter s/ may be specified only once.");
        assertFailure("0 m/2026-13 x/value", "Error: Unknown parameter: x/.");
        assertFailure("1 junk m/2026-10 s/pending", StudentParameters.UNEXPECTED_TEXT);
        assertFailure("1 m/2026-10 extra s/pending", StudentParameters.UNEXPECTED_TEXT);
    }

    @Test
    public void parse_checksIndexThenMonthThenStatus() {
        assertFailure("01 m/2026-13 s/unknown", VisibleIndex.MESSAGE_INVALID);
        assertFailure("999999999999999999999999 m/2026-13 s/unknown", PaymentHistory.MESSAGE_INVALID_MONTH);
        assertFailure("99 s/unknown m/2026-10", PaymentStatus.MESSAGE_INVALID);
        assertFailure("99 m/2026-02-01 s/pending", PaymentHistory.MESSAGE_INVALID_MONTH);
        assertFailure("1 m/2026-10 s/PENDING", PaymentStatus.MESSAGE_INVALID);
    }

    @Test
    public void parse_rejectsControlsAndPreservesUnknownCommandPrecedence() {
        assertThrows(ParseException.class, StudentParameters.UNEXPECTED_TEXT, ()
                -> new TutorConnectParser().parseCommand("payment 1 m/2026-10\n s/pending"));
        assertThrows(ParseException.class, Messages.MESSAGE_UNKNOWN_COMMAND, ()
                -> new TutorConnectParser().parseCommand("Payment 0 m/bad s/bad"));
    }

    @Test
    public void parse_oversizedIndex_isRetainedForSafeResolution() throws Exception {
        String oversized = "9".repeat(100);
        assertEquals(new PaymentCommand(VisibleIndex.parse(oversized), YearMonth.of(2026, 10),
                PaymentStatus.COMPLETED), parser.parse(oversized + " m/2026-10 s/completed"));
    }

    private void assertFailure(String args, String message) {
        assertThrows(ParseException.class, message, () -> parser.parse(args));
    }
}
