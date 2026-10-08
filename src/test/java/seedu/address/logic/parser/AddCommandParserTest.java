package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;

public class AddCommandParserTest {
    private final AddCommandParser parser = new AddCommandParser();

    @Test
    public void parse_allParameterPermutations_success() {
        String[] fields = {"n/Alex Tan", "p/+65-8123-4567", "a/21 Clementi Ave 3 #04-18"};
        Person expected = new Person(new Name("Alex Tan"), new Phone("81234567"),
                new Address("21 Clementi Ave 3 #04-18"));
        for (int first = 0; first < 3; first++) {
            for (int second = 0; second < 3; second++) {
                if (first != second) {
                    int third = 3 - first - second;
                    assertParseSuccess(parser, "  " + fields[first] + "   " + fields[second] + "  "
                            + fields[third] + " ", new AddCommand(expected));
                }
            }
        }
    }

    @Test
    public void parse_unicodeNormalizationAndEmbeddedSlash_success() {
        Person expected = new Person(new Name("Éva Tan"), new Phone("81234567"), new Address("12/3 A & B"));
        assertParseSuccess(parser, " n/Ｅ́va　 Tan p/８１２３４５６７ a/１２/３  A & B", new AddCommand(expected));
    }

    @Test
    public void parse_structuralErrors_precedeValues() {
        assertParseFailure(parser, " n/123 p/bad a/--- x/value", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " text n/Alex p/81234567 a/A", "Error: Unexpected text after command.");
        assertParseFailure(parser, " N/Alex p/81234567 a/A", "Error: Unknown parameter: N/.");
        assertParseFailure(parser, " n/Alex p/81234567 a/A e/a@b.com", "Error: Unknown parameter: e/.");
        assertParseFailure(parser, " n/Alex p/81234567 a/A t/friend", "Error: Unknown parameter: t/.");
        assertParseFailure(parser, "", "Error: Missing required parameter: n/NAME.");
        assertParseFailure(parser, " n/123 a/A", "Error: Missing required parameter: p/PHONE.");
        assertParseFailure(parser, " n/Alex p/bad", "Error: Missing required parameter: a/ADDRESS.");
        String valid = " n/Alex p/81234567 a/A";
        for (String prefix : new String[]{"n/", "p/", "a/"}) {
            assertParseFailure(parser, valid + " " + prefix,
                    "Error: Parameter " + prefix + " may be specified only once.");
        }
    }

    @Test
    public void parse_blankValuesAreInvalid_notMissing() {
        assertParseFailure(parser, " n/ p/81234567 a/A", Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " n/Alex p/ a/A", Phone.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " n/Alex p/81234567 a/ ", Address.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidValues_followDocumentedOrder() {
        assertParseFailure(parser, " a/--- p/bad n/123", Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " a/--- p/bad n/Alex", Phone.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " a/--- p/81234567 n/Alex", Address.MESSAGE_CONSTRAINTS);
        for (String control : new String[]{"\n", "\r", "\t", "\u0000", "\u2028", "\u200b"}) {
            assertParseFailure(parser, " n/Alex" + control + " p/81234567 a/A", Name.MESSAGE_CONSTRAINTS);
            assertParseFailure(parser, " n/Alex p/81234567 a/A" + control, Address.MESSAGE_CONSTRAINTS);
        }
    }
}
