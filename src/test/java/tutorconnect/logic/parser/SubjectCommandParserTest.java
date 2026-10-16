package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.SubjectCommand;
import tutorconnect.model.student.Subject;

public class SubjectCommandParserTest {

    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";

    private final SubjectCommandParser parser = new SubjectCommandParser();

    private static SubjectCommand command(int index, String subject) {
        return new SubjectCommand(Index.fromOneBased(index), new Subject(subject));
    }

    @Test
    public void parse_validInput_success() {
        assertParseSuccess(parser, " 1 s/O-Level Chemistry", command(1, "O-Level Chemistry"));
        assertParseSuccess(parser, " 2 s/H2 Math", command(2, "H2 Math"));
        assertParseSuccess(parser, " 3 s/Primary 6 English", command(3, "Primary 6 English"));
        assertParseSuccess(parser, "   1    s/   H2   Math   ", command(1, "H2 Math"));
    }

    @Test
    public void parse_slashesInsideSubject_areKeptAsData() {
        assertParseSuccess(parser, " 1 s/Math/Science", command(1, "Math/Science"));
        assertParseSuccess(parser, " 1 s/English (Oral) / Writing", command(1, "English (Oral) / Writing"));
        assertParseSuccess(parser, " 1 s/Physics 2/3", command(1, "Physics 2/3"));
    }

    @Test
    public void parse_symbolsAndDisplayCase_arePreserved() {
        assertParseSuccess(parser, " 1 s/Arts & Crafts", command(1, "Arts & Crafts"));
        assertParseSuccess(parser, " 1 s/Children's Literature", command(1, "Children's Literature"));
        assertParseSuccess(parser, " 1 s/h2 MATH", command(1, "h2 MATH"));
    }

    @Test
    public void parse_invalidSubject_failure() {
        for (String subject : new String[] {"", "   ", "A", "12", "Math@Home", "Math!", "a".repeat(51)}) {
            assertParseFailure(parser, " 1 s/" + subject, Subject.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_missingParameter_failure() {
        assertParseFailure(parser, "", "Error: Missing required parameter: s/SUBJECT.");
        assertParseFailure(parser, " 1", "Error: Missing required parameter: s/SUBJECT.");
    }

    @Test
    public void parse_missingIndex_failure() {
        assertParseFailure(parser, " s/Math", INVALID_INDEX);
        assertParseFailure(parser, "s/Math", INVALID_INDEX);
    }

    @Test
    public void parse_repeatedParameter_failure() {
        assertParseFailure(parser, " 1 s/Math s/Physics", "Error: Parameter s/ may be specified only once.");
    }

    @Test
    public void parse_unknownParameterOrUnexpectedText_failure() {
        assertParseFailure(parser, " 1 x/foo", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 1 s/Math l/Sec4", "Error: Unknown parameter: l/.");
        assertParseFailure(parser, " 1 foo s/Math", "Error: Unexpected text after command.");
    }

    @Test
    public void parse_invalidIndex_failure() {
        for (String index : new String[] {"0", "-1", "01", "abc", "1.5"}) {
            assertParseFailure(parser, " " + index + " s/Math", INVALID_INDEX);
        }
    }

    @Test
    public void parse_errorPrecedence() {
        assertParseFailure(parser, " abc x/1 s/@", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " abc s/@ s/@@", "Error: Parameter s/ may be specified only once.");
        // Index syntax is checked before the subject value.
        assertParseFailure(parser, " 0 s/@", INVALID_INDEX);
        assertParseFailure(parser, " abc s/@", INVALID_INDEX);
    }

    @Test
    public void parse_unicodeSubjectValuesAndDocumentedExamples_success() {
        assertParseSuccess(parser, " 1 s/English Language", command(1, "English Language"));
        assertParseSuccess(parser, " 1 s/Physics & Chemistry", command(1, "Physics & Chemistry"));
        assertParseSuccess(parser, " 1 s/\u00a0Ｈ２\u2002\u202fMath\u3000", command(1, "H2 Math"));
        for (String value : new String[] {"Math\t", "Math\n", "\u0000Math", "Math\u200b", "Math\u2029"}) {
            assertParseFailure(parser, " 1 s/" + value, Subject.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_structuralErrorsTakePriorityOverIndexAndSubject() {
        assertParseFailure(parser, " 0 extra s/@ x/", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 0 extra s/ s/", "Error: Parameter s/ may be specified only once.");
        assertParseFailure(parser, " 0 extra s/@", "Error: Unexpected text after command.");
        assertParseFailure(parser, " 0", "Error: Missing required parameter: s/SUBJECT.");
        assertParseFailure(parser, " 0 s/", INVALID_INDEX);
        assertParseFailure(parser, " 1 s/Math s/", "Error: Parameter s/ may be specified only once.");
        assertParseFailure(parser, " 1 s/ s/Math", "Error: Parameter s/ may be specified only once.");
        assertParseFailure(parser, " 1 S/Math", "Error: Unknown parameter: S/.");
        assertParseFailure(parser, " 1 s/Math q2/foo", "Error: Unknown parameter: q2/.");
    }

    @Test
    public void parse_indexSyntaxAndOversizedIndex_doNotOverflow() {
        for (String index : new String[] {"+1", "00", "1e2", "1/2", "1s/Math"}) {
            assertParseFailure(parser, " " + index + " s/Math", INVALID_INDEX);
        }
        String huge = "9".repeat(100);
        assertParseSuccess(parser, " " + huge + " s/Math",
                new SubjectCommand(Index.fromOneBased(huge), new Subject("Math")));
        assertParseFailure(parser, " " + huge + " s/@", Subject.MESSAGE_CONSTRAINTS);
    }
}
