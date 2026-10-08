package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.LevelCommand;
import seedu.address.model.person.EducationLevel;

public class LevelCommandParserTest {

    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";

    private final LevelCommandParser parser = new LevelCommandParser();

    private static LevelCommand command(int index, String level) {
        return new LevelCommand(Index.fromOneBased(index), new EducationLevel(level));
    }

    @Test
    public void parse_validInput_success() {
        assertParseSuccess(parser, " 1 l/Secondary 4", command(1, "Secondary 4"));
        assertParseSuccess(parser, " 2 l/JC2", command(2, "JC 2"));
        assertParseSuccess(parser, " 3 l/Primary 6", command(3, "Primary 6"));
        assertParseSuccess(parser, "1 l/sec4", new LevelCommand(INDEX_FIRST_PERSON, new EducationLevel("Sec 4")));
        assertParseSuccess(parser, "   2    l/   P 3   ", command(2, "Primary 3"));
        assertParseSuccess(parser, "\t2\tl/s5", new LevelCommand(INDEX_SECOND_PERSON, new EducationLevel("Sec5")));
    }

    @Test
    public void parse_everyAcceptedLevel_success() {
        for (int grade = 1; grade <= 6; grade++) {
            assertParseSuccess(parser, " 1 l/P" + grade, command(1, "Primary " + grade));
        }
        for (int grade = 1; grade <= 5; grade++) {
            assertParseSuccess(parser, " 1 l/Sec" + grade, command(1, "Secondary " + grade));
            assertParseSuccess(parser, " 1 l/S " + grade, command(1, "Secondary " + grade));
        }
        for (int grade = 1; grade <= 2; grade++) {
            assertParseSuccess(parser, " 1 l/JC" + grade, command(1, "JC " + grade));
        }
    }

    @Test
    public void parse_invalidLevel_failure() {
        for (String level : new String[] {"Sec 6", "Primary 7", "JC3", "Sec Four", "Preschool", "University",
            "", "   ", "Secondary 4 Express"}) {
            assertParseFailure(parser, " 1 l/" + level, EducationLevel.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_missingParameter_failure() {
        assertParseFailure(parser, "", "Error: Missing required parameter: l/LEVEL.");
        assertParseFailure(parser, " 1", "Error: Missing required parameter: l/LEVEL.");
        assertParseFailure(parser, " 1   ", "Error: Missing required parameter: l/LEVEL.");
    }

    @Test
    public void parse_missingIndex_failure() {
        assertParseFailure(parser, " l/Sec4", INVALID_INDEX);
        assertParseFailure(parser, "l/Sec4", INVALID_INDEX);
        assertParseFailure(parser, " l/", INVALID_INDEX);
    }

    @Test
    public void parse_repeatedParameter_failure() {
        assertParseFailure(parser, " 1 l/Sec4 l/Sec5", "Error: Parameter l/ may be specified only once.");
    }

    @Test
    public void parse_unknownParameterOrUnexpectedText_failure() {
        assertParseFailure(parser, " 1 x/foo", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 1 l/Sec4 s/Math", "Error: Unknown parameter: s/.");
        assertParseFailure(parser, " 1 foo l/Sec4", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 1 foo", "Error: Unexpected text after command.");
    }

    @Test
    public void parse_invalidIndex_failure() {
        for (String index : new String[] {"0", "-1", "01", "abc", "1.5", "+1"}) {
            assertParseFailure(parser, " " + index + " l/Sec4", INVALID_INDEX);
        }
    }

    @Test
    public void parse_errorPrecedence() {
        // Unknown parameter beats a missing or invalid index and an invalid value.
        assertParseFailure(parser, " abc x/1 l/Bad", "Error: Unknown parameter: x/.");
        // Repeated parameter beats an invalid index and invalid values.
        assertParseFailure(parser, " abc l/Bad l/Worse", "Error: Parameter l/ may be specified only once.");
        // Index syntax is checked before the level value.
        assertParseFailure(parser, " 0 l/Bad", INVALID_INDEX);
        assertParseFailure(parser, " abc l/Bad", INVALID_INDEX);
    }

    @Test
    public void parse_veryLargeIndex_isSyntacticallyValid() throws Exception {
        String huge = "9".repeat(100);
        assertParseSuccess(parser, " " + huge + " l/Sec4",
                new LevelCommand(ParserUtil.parseIndex(huge), new EducationLevel("Sec4")));
    }
}
