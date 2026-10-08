package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ListCommand;

class ListCommandParserTest {
    @Test
    void parse_noParameters_success() throws Exception {
        assertInstanceOf(ListCommand.class, new ListCommandParser().parse("   "));
        assertInstanceOf(ListCommand.class, new AddressBookParser().parseCommand("  list   "));
    }

    @Test
    void parse_anyParameter_rejected() {
        for (String value : java.util.List.of("1", "word", "n/name", "  1 2", "\n")) {
            assertParseFailure(new ListCommandParser(), value, ListCommandParser.MESSAGE_PARAMETERS);
        }
    }
}
