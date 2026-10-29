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
    void parseCommand_mistypedDelete_isRefusedBeforeAnythingIsCleared() {
        TutorConnectParser parser = new TutorConnectParser();
        assertThrows(ParseException.class, ClearCommandParser.MESSAGE_PARAMETERS, () -> parser.parseCommand("clear 1"));
    }
}
