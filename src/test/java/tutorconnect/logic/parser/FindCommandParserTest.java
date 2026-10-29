package tutorconnect.logic.parser;

import static tutorconnect.logic.Messages.MESSAGE_MISSING_REQUIRED_PARAMETER;
import static tutorconnect.logic.Messages.MESSAGE_UNKNOWN_PARAMETER;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.logic.commands.FindCommand;
import tutorconnect.model.student.NameContainsKeywordsPredicate;

public class FindCommandParserTest {

    private FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "     ", String.format(MESSAGE_MISSING_REQUIRED_PARAMETER, "KEYWORD"));
        assertParseFailure(parser, "", "Error: Missing required parameter: KEYWORD.");
    }

    @Test
    public void parse_unicodeSpaceAndCompatibilityCharacters_normalizesKeywords() {
        assertParseSuccess(parser, "\u00a0ＡＬＩ\u2003Ｂｏ\u00a0",
                new FindCommand(new NameContainsKeywordsPredicate(List.of("ali", "bo"))));
        assertParseFailure(parser, "\u00a0\u2003", String.format(MESSAGE_MISSING_REQUIRED_PARAMETER, "KEYWORD"));
        assertParseFailure(parser, "Ali\u200bce", StudentParameters.UNEXPECTED_TEXT);
    }

    @Test
    public void parse_validArgs_returnsFindCommand() {
        // no leading and trailing whitespaces
        FindCommand expectedFindCommand =
                new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice", "Bob")));
        assertParseSuccess(parser, "Alice Bob", expectedFindCommand);

        // multiple whitespaces between keywords
        assertParseSuccess(parser, " \n Alice \n \t Bob  \t", expectedFindCommand);
    }

    @Test
    public void parse_parameterStyleKeyword_throwsParseException() {
        assertParseFailure(parser, " n/Alex", String.format(MESSAGE_UNKNOWN_PARAMETER, "n/"));
        assertParseFailure(parser, " Alex n/Bob", String.format(MESSAGE_UNKNOWN_PARAMETER, "n/"));
        assertParseFailure(parser, " a/b", String.format(MESSAGE_UNKNOWN_PARAMETER, "a/"));
        assertParseFailure(parser, " name/Alex", String.format(MESSAGE_UNKNOWN_PARAMETER, "name/"));
        assertParseFailure(parser, " Alex/", String.format(MESSAGE_UNKNOWN_PARAMETER, "Alex/"));
    }

    @Test
    public void parse_digitsBeforeSlash_stillTreatedAsKeyword() {
        assertParseSuccess(parser, " 12/3",
                new FindCommand(new NameContainsKeywordsPredicate(List.of("12/3"))));
    }

}
