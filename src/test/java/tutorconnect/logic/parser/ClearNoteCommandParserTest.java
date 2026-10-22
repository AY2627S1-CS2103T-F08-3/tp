package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.ClearNoteCommand;

public class ClearNoteCommandParserTest {

    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";

    private final ClearNoteCommandParser parser = new ClearNoteCommandParser();

    @Test
    public void parse_oneIndex_success() {
        assertParseSuccess(parser, " 1", new ClearNoteCommand(Index.fromOneBased(1)));
        assertParseSuccess(parser, "2", new ClearNoteCommand(Index.fromOneBased(2)));
        assertParseSuccess(parser, "   3   ", new ClearNoteCommand(Index.fromOneBased(3)));
        assertParseSuccess(parser, " ４ ", new ClearNoteCommand(Index.fromOneBased(4)));
        assertParseSuccess(parser, " 5 ", new ClearNoteCommand(Index.fromOneBased(5)));
    }

    @Test
    public void parse_veryLargeIndex_isSyntacticallyValid() throws Exception {
        String huge = "9".repeat(100);
        assertParseSuccess(parser, " " + huge, new ClearNoteCommand(ParserUtil.parseIndex(huge)));
    }

    @Test
    public void parse_missingIndex_failure() {
        assertParseFailure(parser, "", "Error: Missing required parameter: INDEX.");
        assertParseFailure(parser, "     ", "Error: Missing required parameter: INDEX.");
    }

    @Test
    public void parse_extraText_failure() {
        assertParseFailure(parser, " 1 2", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 foo", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 m/note", "Error: Unexpected text after command.");
    }

    @Test
    public void parse_invalidIndex_failure() {
        for (String index : new String[] {"0", "-1", "01", "abc", "1.5", "+1", "1e2", "m/note", "\u0000"}) {
            assertParseFailure(parser, " " + index, INVALID_INDEX);
        }
    }
}
