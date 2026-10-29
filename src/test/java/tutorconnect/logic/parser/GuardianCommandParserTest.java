package tutorconnect.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.GuardianCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.GuardianPhone;
import tutorconnect.model.student.Phone;

/** Guardian-specific boundaries and precedence, without changing other field parsers. */
public class GuardianCommandParserTest {
    private final GuardianCommandParser parser = new GuardianCommandParser();

    @Test
    public void parse_validPhones_reusesStudentPhoneNormalization() throws Exception {
        for (String phone : new String[] {"61234567", "81234567", "91234567", "+65 8123 4567",
            "+65-8123-4567", "  8123   4567  ", "＋６５ ８１２３ ４５６７", "8123\u00a04567",
            "8123\u16804567", "8123\u20074567"}) {
            GuardianPhone expected = new GuardianPhone(new Phone(phone).value);
            assertParseSuccess(parser, "  1    g/" + phone, new GuardianCommand(Index.fromOneBased(1), expected));
            assertEquals(expected, ParserUtil.parseGuardianPhone(phone));
        }
        assertParseSuccess(parser, "\u00a0１\u00a0g/81234567  ",
                new GuardianCommand(Index.fromOneBased(1), new GuardianPhone("+6581234567")));
    }

    @Test
    public void parse_invalidPhones_rejectsBlankMalformedAndProhibitedCharacters() {
        for (String phone : new String[] {"", "   ", "12345678", "71234567", "8123456", "812345678",
            "+60 81234567", "65 81234567", "8123--4567", "81234567g/91234567", "81234567 extra",
            "9".repeat(100), "8123\n4567", "81234567\r", "\t81234567", "8123\u00004567",
            "8123\u200b4567", "8123\u20284567", "8123\u20294567", "8123\ud8004567"}) {
            assertParseFailure(parser, " 1 g/" + phone, ParserUtil.MESSAGE_INVALID_GUARDIAN_PHONE);
        }
    }

    @Test
    public void parse_structureErrors_precedeIndexAndPhoneValidation() {
        for (String args : new String[] {"", " 1", " bad"}) {
            assertParseFailure(parser, args, "Error: Missing required parameter: g/PHONE.");
        }
        assertParseFailure(parser, " 0 g/bad g/worse", "Error: Parameter g/ may be specified only once.");
        assertParseFailure(parser, " 0 g/bad g/worse x/value", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 0 extra g/bad", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 0 extra", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 G/81234567", "Error: Unknown parameter: G/.");
        assertParseFailure(parser, " 1 ｇ/81234567", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 g／81234567", "Error: Unexpected text after command.");
    }

    @Test
    public void parse_invalidIndices_precedePhoneValidation() {
        assertParseFailure(parser, "  g/bad", ParserUtil.MESSAGE_MISSING_INDEX);
        for (String index : new String[] {"0", "01", "-1", "+1", "1.5", "abc", "1\u200b"}) {
            assertParseFailure(parser, " " + index + " g/bad", ParserUtil.MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_oversizedIndex_isNotNarrowedBeforeListResolution() throws Exception {
        String index = "9".repeat(100);
        assertParseSuccess(parser, " " + index + " g/81234567",
                new GuardianCommand(Index.fromOneBased(index), new GuardianPhone("+6581234567")));
    }

    @Test
    public void parseCommand_commandSyntaxRemainsCaseSensitive() throws Exception {
        TutorConnectParser commands = new TutorConnectParser();
        assertEquals(parser.parse(" 1 g/81234567"), commands.parseCommand("  guardian  1 g/81234567  "));
        for (String command : new String[] {"Guardian", "GUARDIAN", "ｇｕａｒｄｉａｎ"}) {
            ParseException error = assertThrows(ParseException.class, ()
                -> commands.parseCommand(command + " 0 x/bad"));
            assertEquals("Error: Unknown command. Type help to view available commands.", error.getMessage());
        }
    }
}
