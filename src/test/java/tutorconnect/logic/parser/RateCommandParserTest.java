package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.RateCommand;
import tutorconnect.model.student.HourlyRate;

public class RateCommandParserTest {
    private final RateCommandParser parser = new RateCommandParser();

    @Test
    public void parse_validAmounts_normalizeToCents() {
        for (String amount : List.of("1", "1.0", "1.00", "19.99", "999.99", "1000", "1000.00")) {
            assertParseSuccess(parser, " 1 r/" + amount, command(amount));
        }
        assertParseSuccess(parser, "   1    r/  40.0   ", command("40.00"));
        assertParseSuccess(parser, "\t1\tr/40", command("40"));
        assertParseSuccess(parser, " １ ｒ/４０．５０", command("40.50"));
    }

    @Test
    public void parse_invalidAmounts_reportsRateError() {
        for (String amount : List.of("", " ", "-1", "0", "0.99", "1000.01", "99999999999999999999999",
                "1.001", "40.000", "1000.000", "forty", "NaN", "Infinity", "+40", "01", "040.00",
                ".50", "40.", "1e2", "$40", "S$40", "1,000.00", "40,50", "40 50", "40r/50",
                "40\n", "40\t", "4\u00000")) {
            assertParseFailure(parser, " 1 r/" + amount, ParserUtil.MESSAGE_INVALID_HOURLY_RATE);
        }
    }

    @Test
    public void parse_structureAndIndexErrors_areSpecific() {
        for (String input : List.of("", " 1", " 1   ")) {
            assertParseFailure(parser, input, "Error: Missing required parameter: r/RATE.");
        }
        for (String input : List.of("r/40", " r/")) {
            assertParseFailure(parser, input, ParserUtil.MESSAGE_MISSING_INDEX);
        }
        for (String input : List.of(" 0 r/40", " -1 r/40", " 01 r/40",
                " +1 r/40", " 1.5 r/40", " abc r/40")) {
            assertParseFailure(parser, input, ParserUtil.MESSAGE_INVALID_INDEX);
        }
        assertParseFailure(parser, " 1r/40", "Error: Missing required parameter: r/RATE.");
        assertParseFailure(parser, " 1 40", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 extra r/40", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 R/40", "Error: Unknown parameter: R/.");
        assertParseFailure(parser, " 1 r/40 x/50", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 1 r/40 r/50", "Error: Parameter r/ may be specified only once.");
        assertParseFailure(parser, " 1 r/ r/", "Error: Parameter r/ may be specified only once.");
    }

    @Test
    public void parse_multipleErrors_followExistingSharedPriority() {
        assertParseFailure(parser, " abc extra r/bad r/worse x/1", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " abc extra r/bad r/worse", "Error: Parameter r/ may be specified only once.");
        assertParseFailure(parser, " abc extra r/bad", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 0 r/bad", ParserUtil.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, " 99 r/bad", ParserUtil.MESSAGE_INVALID_HOURLY_RATE);
    }

    @Test
    public void parse_oversizedIndex_isValidatedWithoutIntegerOverflow() throws Exception {
        String index = "9".repeat(100);
        assertParseSuccess(parser, " " + index + " r/40",
                new RateCommand(ParserUtil.parseIndex(index), new HourlyRate(new BigDecimal("40"))));
    }

    private RateCommand command(String amount) {
        return new RateCommand(Index.fromOneBased(1), new HourlyRate(new BigDecimal(amount)));
    }
}
