package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import tutorconnect.logic.commands.FilterCommand;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.StudentCriteriaPredicate;
import tutorconnect.model.student.Subject;

class FilterCommandParserTest {
    private final FilterCommandParser parser = new FilterCommandParser();

    @Test
    void parse_validCriteria_canonicalizesValuesInEitherOrder() {
        FilterCommand both = command("Secondary 4", "H2 Math");
        assertParseSuccess(parser, " l/sec4 s/  h2   MATH ", both);
        assertParseSuccess(parser, " s/Ｈ２\u00a0Math l/Ｓ４", both);
        assertParseSuccess(parser, " l/p1", command("Primary 1", null));
        assertParseSuccess(parser, " s/Physics", command(null, "Physics"));
        assertParseSuccess(parser, " s/English/Literature", command(null, "English/Literature"));
    }

    @Test
    void parse_invalidStructure_rejectsBeforeValues() {
        for (String input : new String[] {"", "  ", "\u00a0"}) {
            assertParseFailure(parser, input, FilterCommand.MESSAGE_MISSING_CRITERIA);
        }
        assertParseFailure(parser, " l/p1 x/foo", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " l/p1 subject/Math", "Error: Unknown parameter: subject/.");
        assertParseFailure(parser, " L/p1", "Error: Unknown parameter: L/.");
        assertParseFailure(parser, " l/bad l/p1", "Error: Parameter l/ may be specified only once.");
        assertParseFailure(parser, " s/Math s/Math", "Error: Parameter s/ may be specified only once.");
        assertParseFailure(parser, " bare l/p1", StudentParameters.UNEXPECTED_TEXT);
        assertParseFailure(parser, " l/p1\n", StudentParameters.UNEXPECTED_TEXT);
    }

    @Test
    void parse_invalidValues_reportsOwnerConstraints() {
        for (String input : new String[] {" l/", " l/p7", " l/sec6", " l/college"}) {
            assertParseFailure(parser, input, EducationLevel.MESSAGE_CONSTRAINTS);
        }
        for (String input : new String[] {" s/", " s/1", " s/Math!"}) {
            assertParseFailure(parser, input, Subject.MESSAGE_CONSTRAINTS);
        }
    }

    private FilterCommand command(String level, String subject) {
        return new FilterCommand(new StudentCriteriaPredicate(Optional.ofNullable(level).map(EducationLevel::new),
                Optional.ofNullable(subject).map(Subject::new)));
    }
}
