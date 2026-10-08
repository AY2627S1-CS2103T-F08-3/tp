package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.DeleteCommand;

/**
 * As we are only doing white-box testing, our test cases do not cover path variations
 * outside of the DeleteCommand code. For example, inputs "1" and "1 abc" take the
 * same path through the DeleteCommand, and therefore we test only one of them.
 * The path variation for those two cases occurs inside the ParserUtil, and
 * therefore should be covered by the ParserUtilTest.
 */
public class DeleteCommandParserTest {

    private DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_validArgs_returnsDeleteCommand() {
        assertParseSuccess(parser, "1", new DeleteCommand(INDEX_FIRST_PERSON));
    }

    @Test
    public void parse_invalidArgs_throwsParseException() {
        assertParseFailure(parser, "a", VisibleIndex.MESSAGE_INVALID);
    }

    @Test
    public void parse_missingMultipleAndMalformedIndices_exactErrors() {
        assertParseFailure(parser, "   ", DeleteCommandParser.MESSAGE_MISSING);
        assertParseFailure(parser, "1   2", DeleteCommandParser.MESSAGE_MULTIPLE);
        for (String value : java.util.List.of("0", "01", "+1", "-1", "1.5", "1\n", "1\t2", "1\u0000")) {
            assertParseFailure(parser, value, VisibleIndex.MESSAGE_INVALID);
        }
        assertParseSuccess(parser, "   1   ", new DeleteCommand(INDEX_FIRST_PERSON));
        assertParseSuccess(parser, "\uff11", new DeleteCommand(INDEX_FIRST_PERSON));
    }
}
