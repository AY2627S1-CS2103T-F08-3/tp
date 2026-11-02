package tutorconnect.logic.parser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.logic.commands.ClearCommand;
import tutorconnect.logic.parser.exceptions.ParseException;

class ClearCommandParserTest {
    @Test
    void parse_noParameters_success() throws Exception {
        assertInstanceOf(ClearCommand.class, new ClearCommandParser().parse(""));
        assertInstanceOf(ClearCommand.class, new ClearCommandParser().parse("   "));
        assertInstanceOf(ClearCommand.class, new TutorConnectParser().parseCommand("  clear   "));
    }

    @Test
    void parse_anyParameter_rejected() {
        for (String value : List.of(" 1", " 3", " all", " n/name", "  1 2", " x")) {
            assertParseFailure(new ClearCommandParser(), value, ClearCommandParser.MESSAGE_PARAMETERS);
        }
    }

    @Test
    void parse_hiddenLineBreaks_rejectedBeforeClearing() {
        for (String separator : List.of("\n", "\r", "\t", "\u0085", "\u2028", "\u2029")) {
            assertParseFailure(new ClearCommandParser(), separator, ClearCommandParser.MESSAGE_PARAMETERS);
            assertThrows(ParseException.class, ClearCommandParser.MESSAGE_PARAMETERS, () ->
                    new TutorConnectParser().parseCommand("clear" + separator));
            for (String input : List.of(separator + "clear", " " + separator + "clear ")) {
                assertThrows(ParseException.class, () -> new TutorConnectParser().parseCommand(input));
            }
        }
    }

    @Test
    void parseCommand_mistypedDelete_isRefusedBeforeAnythingIsCleared() {
        TutorConnectParser parser = new TutorConnectParser();
        assertThrows(ParseException.class, ClearCommandParser.MESSAGE_PARAMETERS, () -> parser.parseCommand("clear 1"));
    }
}
