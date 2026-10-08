package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.ScheduleCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.WeeklySlot;

public class ScheduleCommandParserTest {
    private final ScheduleCommandParser parser = new ScheduleCommandParser();

    @Test
    public void parse_reorderedPrefixesAndWhitespace_sameCommand() throws Exception {
        ScheduleCommand expected = new ScheduleCommand(Index.fromOneBased(1),
                new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(19, 0)));
        for (String args : List.of(" 1 d/Tuesday t/19:00", " 1 t/19:00 d/tUe", "  1   d/ tue   t/ 19:00  ",
                "\u00a0１ d/Ｔｕｅ t/１９：００  ", " １\u00a0t/\u00a0１９：００ d/\u00a0Ｔｕｅ\u00a0")) {
            assertEquals(expected, parser.parse(args));
        }
        assertEquals(expected, new AddressBookParser().parseCommand("  schedule  1 t/19:00 d/Tue  "));
        assertEquals(expected, new AddressBookParser().parseCommand("\u00a0schedule 1 d/Tue t/19:00"));
    }

    @Test
    public void parse_weekendExample_success() throws Exception {
        assertEquals(new ScheduleCommand(Index.fromOneBased(2),
                new WeeklySlot(DayOfWeek.SATURDAY, LocalTime.of(9, 30))), parser.parse(" 2 d/Sat t/09:30"));
    }

    @Test
    public void parse_missingAndRepeatedParameters_exactMessages() {
        assertFailure(" 1", "Error: Missing required parameter: d/DAY.");
        assertFailure(" 1 t/09:00", "Error: Missing required parameter: d/DAY.");
        assertFailure(" 1 d/Mon", "Error: Missing required parameter: t/TIME.");
        assertFailure(" 1 d/Mon d/Tue t/09:00", "Error: Parameter d/ may be specified only once.");
        assertFailure(" 1 t/09:00 d/Mon t/10:00", "Error: Parameter t/ may be specified only once.");
        assertFailure(" 1 t/09:00 t/10:00", "Error: Missing required parameter: d/DAY.");
    }

    @Test
    public void parse_unknownPrefixesAndUnexpectedText_precedeMissingOrInvalidValues() {
        assertFailure(" 0 d/Funday x/value", "Error: Unknown parameter: x/.");
        assertFailure(" 1 D/Mon t/09:00", "Error: Unknown parameter: D/.");
        assertFailure(" 1 d/Mon T/09:00", "Error: Unknown parameter: T/.");
        assertFailure(" 1 day/Mon t/09:00", "Error: Unknown parameter: day/.");
        assertFailure(" 1 2 d/Mon t/09:00", StudentParameters.UNEXPECTED_TEXT);
        assertFailure(" 1 extra d/Mon", StudentParameters.UNEXPECTED_TEXT);
        assertFailure(" 1 d/Mon t/09:00 extra", StudentParameters.UNEXPECTED_TEXT);
        assertFailure(" 1 d/Mon extra t/09:00", StudentParameters.UNEXPECTED_TEXT);
    }

    @Test
    public void parse_invalidIndexSyntax_precedesDayAndTime() {
        for (String index : List.of("", "0", "01", "+1", "-1", "1.0", "x")) {
            assertFailure(" " + index + " t/24:00 d/Funday", ParserUtil.MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_invalidDayThenTime_regardlessOfPrefixOrder() {
        assertFailure(" 999 t/24:00 d/Funday", WeeklySlot.MESSAGE_INVALID_DAY);
        assertFailure(" 999 d/Funday t/24:00", WeeklySlot.MESSAGE_INVALID_DAY);
        assertFailure(" 999 t/24:00 d/Mon", WeeklySlot.MESSAGE_INVALID_TIME);
        assertFailure(" 1 d/ t/09:00", WeeklySlot.MESSAGE_INVALID_DAY);
        assertFailure(" 1 d/Mon t/", WeeklySlot.MESSAGE_INVALID_TIME);
    }

    @Test
    public void parse_oversizedIndex_isSafeAndRetainsExactIndex() throws Exception {
        String index = "99999999999999999999999999999999999999";
        assertEquals(new ScheduleCommand(Index.fromOneBased(index),
                new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(23, 59))),
                parser.parse(" " + index + " t/23:59 d/Sun"));
    }

    @Test
    public void parse_prohibitedControls_doNotDisappearDuringTrimming() {
        for (String input : List.of("\nschedule 1 d/Mon t/09:00", "schedule 1\n d/Mon t/09:00",
                "schedule 1 d/Mon t/09:00\n", "schedule 1 d/Mon\u200b t/09:00")) {
            assertThrows(ParseException.class, StudentParameters.UNEXPECTED_TEXT, ()
                -> new AddressBookParser().parseCommand(input));
        }
    }

    @Test
    public void parse_unknownCommand_precedesArgumentErrorsAndIsCaseSensitive() {
        for (String input : List.of("Schedule 0 x/value", "SCHEDULE 1 d/Funday t/24:00", "unknown x/value\n")) {
            assertThrows(ParseException.class, Messages.MESSAGE_UNKNOWN_COMMAND, ()
                -> new AddressBookParser().parseCommand(input));
        }
    }

    private void assertFailure(String args, String message) {
        assertThrows(ParseException.class, message, () -> parser.parse(args));
    }
}
