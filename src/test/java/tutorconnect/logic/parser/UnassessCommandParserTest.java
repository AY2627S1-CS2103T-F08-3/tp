package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.UnassessCommand;

public class UnassessCommandParserTest {

    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";
    private static final String INVALID_RECORD = UnassessCommand.MESSAGE_INVALID_RECORD;

    private final UnassessCommandParser parser = new UnassessCommandParser();

    private static UnassessCommand command(int index, int record) {
        return new UnassessCommand(Index.fromOneBased(index), Index.fromOneBased(record));
    }

    @Test
    public void parse_validInput_success() {
        assertParseSuccess(parser, " 1 i/2", command(1, 2));
        assertParseSuccess(parser, "3 i/10", command(3, 10));
        assertParseSuccess(parser, "   2    i/   5   ", command(2, 5));
        assertParseSuccess(parser, " １ i/３", command(1, 3));
    }

    @Test
    public void parse_veryLargeNumbers_areSyntacticallyValid() throws Exception {
        String huge = "9".repeat(100);
        assertParseSuccess(parser, " " + huge + " i/" + huge,
                new UnassessCommand(ParserUtil.parseIndex(huge), ParserUtil.parseIndex(huge)));
    }

    @Test
    public void parse_missingOrRepeatedParameter_failure() {
        assertParseFailure(parser, "", "Error: Missing required parameter: i/RECORD.");
        assertParseFailure(parser, " 1", "Error: Missing required parameter: i/RECORD.");
        assertParseFailure(parser, " 1 i/1 i/2", "Error: Parameter i/ may be specified only once.");
    }

    @Test
    public void parse_unknownParameterOrUnexpectedText_failure() {
        assertParseFailure(parser, " 1 x/1", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 1 i/1 d/2026-10-01", "Error: Unknown parameter: d/.");
        assertParseFailure(parser, " 1 foo i/1", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 foo", "Error: Unexpected text after command.");
    }

    @Test
    public void parse_missingOrInvalidIndex_failure() {
        assertParseFailure(parser, " i/1", INVALID_INDEX);
        for (String index : new String[] {"0", "-1", "01", "abc", "1.5", "+1", "1e2"}) {
            assertParseFailure(parser, " " + index + " i/1", INVALID_INDEX);
        }
    }

    @Test
    public void parse_invalidRecordNumber_failure() {
        for (String record : new String[] {"", "   ", "0", "00", "01", "-1", "+1", "abc", "1.5", "1e2", "1 2",
            "#1", "1\u0000"}) {
            assertParseFailure(parser, " 1 i/" + record, INVALID_RECORD);
        }
    }

    @Test
    public void parse_errorPrecedence() {
        assertParseFailure(parser, " abc x/1 i/bad", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 0 i/bad", INVALID_INDEX);
        assertParseFailure(parser, " 99 i/bad", INVALID_RECORD);
    }
}
