package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_REMARK;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {
    private final RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parse_validArgs_returnsRemarkCommand() {
        assertParseSuccess(parser, " 1 r/Likes swimming. ",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes swimming.")));
        assertParseSuccess(parser, "1 r/Call about 咖啡 & tea!",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Call about 咖啡 & tea!")));
    }

    @Test
    public void parse_emptyOrMissingRemark_returnsClearRemarkCommand() {
        RemarkCommand expected = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""));
        assertParseSuccess(parser, "1 r/", expected);
        assertParseSuccess(parser, "1 r/   ", expected);
        assertParseSuccess(parser, "1", expected);
    }

    @Test
    public void parse_invalidIndex_throwsParseException() {
        String message = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);
        for (String input : new String[]{"", "r/note", "0 r/note", "-1 r/note", "a r/note",
            "1.5 r/note", "2147483648 r/note", "1 extra r/note"}) {
            assertParseFailure(parser, input, message);
        }
    }

    @Test
    public void parse_duplicateRemark_throwsParseException() {
        assertParseFailure(parser, "1 r/first r/second",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_REMARK));
    }
}
