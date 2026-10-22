package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.AssessCommand;
import tutorconnect.model.student.AssessmentRecord;

public class AssessCommandParserTest {

    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";
    private static final String UNEXPECTED_TEXT = "Error: Unexpected text after command.";

    private final AssessCommandParser parser = new AssessCommandParser();

    private static AssessCommand command(int index, String date, String title, String grade) {
        return new AssessCommand(Index.fromOneBased(index), AssessmentRecord.of(date, title, grade));
    }

    @Test
    public void parse_validInput_success() {
        assertParseSuccess(parser, " 1 d/2026-10-01 t/Math Midterm g/85/100",
                command(1, "2026-10-01", "Math Midterm", "85/100"));
        assertParseSuccess(parser, "2 d/2026-12-31 t/P6 English (Oral) g/A1",
                command(2, "2026-12-31", "P6 English (Oral)", "A1"));
        assertParseSuccess(parser, "   3    d/  2026-01-05   t/  Science   Test   g/  B+  ",
                command(3, "2026-01-05", "Science Test", "B+"));
        assertParseSuccess(parser, " 1 d/2026-10-01 t/Quiz g/72.5%", command(1, "2026-10-01", "Quiz", "72.5%"));
        assertParseSuccess(parser, " １ d/２０２６-10-01 t/Quiz g/A",
                command(1, "2026-10-01", "Quiz", "A"));
    }

    @Test
    public void parse_prefixesInAnyOrder_success() {
        AssessCommand expected = command(1, "2026-10-01", "Math Midterm", "85/100");
        assertParseSuccess(parser, " 1 d/2026-10-01 g/85/100 t/Math Midterm", expected);
        assertParseSuccess(parser, " 1 t/Math Midterm d/2026-10-01 g/85/100", expected);
        assertParseSuccess(parser, " 1 t/Math Midterm g/85/100 d/2026-10-01", expected);
        assertParseSuccess(parser, " 1 g/85/100 d/2026-10-01 t/Math Midterm", expected);
        assertParseSuccess(parser, " 1 g/85/100 t/Math Midterm d/2026-10-01", expected);
    }

    @Test
    public void parse_slashesAttachedToThePrefixOrInsideWordsOfDigitsAreText() {
        assertParseSuccess(parser, " 1 d/2026-10-01 t/Math/Science g/85/100",
                command(1, "2026-10-01", "Math/Science", "85/100"));
        assertParseSuccess(parser, " 1 d/2026-10-01 t/Paper 1 / Paper 2 g/7 / 10",
                command(1, "2026-10-01", "Paper 1 / Paper 2", "7 / 10"));
        assertParseSuccess(parser, " 1 d/2026-10-01 t/Part 1/2 g/A", command(1, "2026-10-01", "Part 1/2", "A"));
    }

    @Test
    public void parse_aWordOfLettersThenASlashAfterASpaceIsAParameter() {
        assertParseFailure(parser, " 1 d/2026-10-01 t/Paper Math/Science g/A", "Error: Unknown parameter: Math/.");
    }

    @Test
    public void parse_veryLargeIndex_isSyntacticallyValid() throws Exception {
        String huge = "9".repeat(100);
        assertParseSuccess(parser, " " + huge + " d/2026-10-01 t/Quiz g/A", new AssessCommand(
                ParserUtil.parseIndex(huge), AssessmentRecord.of("2026-10-01", "Quiz", "A")));
    }

    @Test
    public void parse_missingParameter_failure() {
        assertParseFailure(parser, "", "Error: Missing required parameter: d/DATE.");
        assertParseFailure(parser, " 1", "Error: Missing required parameter: d/DATE.");
        assertParseFailure(parser, " 1 t/Quiz g/A", "Error: Missing required parameter: d/DATE.");
        assertParseFailure(parser, " 1 d/2026-10-01 g/A", "Error: Missing required parameter: t/TITLE.");
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz", "Error: Missing required parameter: g/GRADE.");
        assertParseFailure(parser, " 1 g/A", "Error: Missing required parameter: d/DATE.");
    }

    @Test
    public void parse_repeatedParameter_failure() {
        assertParseFailure(parser, " 1 d/2026-10-01 d/2026-10-02 t/Quiz g/A",
                "Error: Parameter d/ may be specified only once.");
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz t/Test g/A",
                "Error: Parameter t/ may be specified only once.");
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz g/A g/B",
                "Error: Parameter g/ may be specified only once.");
    }

    @Test
    public void parse_unknownParameterOrUnexpectedText_failure() {
        assertParseFailure(parser, " 1 x/foo d/2026-10-01 t/Quiz g/A", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz g/A s/Math", "Error: Unknown parameter: s/.");
        assertParseFailure(parser, " 1 D/2026-10-01 t/Quiz g/A", "Error: Unknown parameter: D/.");
        assertParseFailure(parser, " 1 foo d/2026-10-01 t/Quiz g/A", UNEXPECTED_TEXT);
        assertParseFailure(parser, " 1 foo", UNEXPECTED_TEXT);
    }

    @Test
    public void parse_missingOrInvalidIndex_failure() {
        assertParseFailure(parser, " d/2026-10-01 t/Quiz g/A", INVALID_INDEX);
        for (String index : new String[] {"0", "-1", "01", "abc", "1.5", "+1", "1e2", "1/2"}) {
            assertParseFailure(parser, " " + index + " d/2026-10-01 t/Quiz g/A", INVALID_INDEX);
        }
    }

    @Test
    public void parse_invalidValues_reportTheDateThenTheTitleThenTheGrade() {
        assertParseFailure(parser, " 1 d/2026-02-30 t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
        assertParseFailure(parser, " 1 d/2026-2-3 t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
        assertParseFailure(parser, " 1 d/ t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
        assertParseFailure(parser, " 1 d/2026-10-01 t/ g/A", AssessmentRecord.MESSAGE_INVALID_TITLE);
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz! g/A", AssessmentRecord.MESSAGE_INVALID_TITLE);
        assertParseFailure(parser, " 1 d/2026-10-01 t/" + "a".repeat(61) + " g/A",
                AssessmentRecord.MESSAGE_INVALID_TITLE);
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz g/", AssessmentRecord.MESSAGE_INVALID_GRADE);
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz g/A*", AssessmentRecord.MESSAGE_INVALID_GRADE);
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz g/" + "a".repeat(21),
                AssessmentRecord.MESSAGE_INVALID_GRADE);
        // The prefixes can come in any order but the values are still checked as date, title, grade.
        assertParseFailure(parser, " 1 g/! t/@ d/bad", AssessmentRecord.MESSAGE_INVALID_DATE);
        assertParseFailure(parser, " 1 g/! t/@ d/2026-10-01", AssessmentRecord.MESSAGE_INVALID_TITLE);
    }

    @Test
    public void parse_errorPrecedence() {
        // Structure comes first, then the index, then the values.
        assertParseFailure(parser, " abc x/1 d/bad t/@ g/!", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " abc d/bad d/worse t/@ g/!", "Error: Parameter d/ may be specified only once.");
        assertParseFailure(parser, " 0 d/bad t/@ g/!", INVALID_INDEX);
        assertParseFailure(parser, " abc d/bad t/@ g/!", INVALID_INDEX);
        // A bad value wins over a row that does not exist, which is only known when the command runs.
        assertParseFailure(parser, " 99 d/bad t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
    }

    @Test
    public void parse_controlCharactersInValues_areRejectedWithTheFieldMessage() {
        assertParseFailure(parser, " 1 d/2026-10-01 t/Qu\tiz g/A", AssessmentRecord.MESSAGE_INVALID_TITLE);
        assertParseFailure(parser, " 1 d/2026-10-01 t/Quiz g/A\n1", AssessmentRecord.MESSAGE_INVALID_GRADE);
        assertParseFailure(parser, " 1 d/2026-10-01\u0000 t/Quiz g/A", AssessmentRecord.MESSAGE_INVALID_DATE);
    }
}
