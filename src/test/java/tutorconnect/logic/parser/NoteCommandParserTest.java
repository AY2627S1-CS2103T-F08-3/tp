package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.NoteCommand;
import tutorconnect.model.student.AcademicNote;

public class NoteCommandParserTest {

    private static final String INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";
    private static final String MISSING_NOTE = "Error: Missing required parameter: m/NOTE.";
    private static final String UNEXPECTED_TEXT = "Error: Unexpected text after command.";

    private final NoteCommandParser parser = new NoteCommandParser();

    private static NoteCommand command(int index, String note) {
        return new NoteCommand(Index.fromOneBased(index), new AcademicNote(note));
    }

    @Test
    public void parse_validInput_success() {
        assertParseSuccess(parser, " 1 m/Weak in calculus", command(1, "Weak in calculus"));
        assertParseSuccess(parser, "2 m/short attention span", command(2, "short attention span"));
        assertParseSuccess(parser, "   3    m/   spaced     note   ", command(3, "spaced note"));
        assertParseSuccess(parser, "\t1\tm/tabs before", command(1, "tabs before"));
        assertParseSuccess(parser, " 1 m/nbsp separators", command(1, "nbsp separators"));
        assertParseSuccess(parser, " 1 m/A", command(1, "A"));
    }

    @Test
    public void parse_everythingAfterTheFirstPrefixIsTheNote() {
        assertParseSuccess(parser, " 1 m/Prefers visuals and/or diagrams",
                command(1, "Prefers visuals and/or diagrams"));
        assertParseSuccess(parser, " 1 m/he/she w/ parent", command(1, "he/she w/ parent"));
        assertParseSuccess(parser, " 1 m/speed in m/s", command(1, "speed in m/s"));
        assertParseSuccess(parser, " 1 m/second m/ prefix is text", command(1, "second m/ prefix is text"));
        assertParseSuccess(parser, " 1 m/x/y and n/a or t/b", command(1, "x/y and n/a or t/b"));
        assertParseSuccess(parser, " 1 m/Needs 2x practice (p.4) - 50% done!",
                command(1, "Needs 2x practice (p.4) - 50% done!"));
    }

    @Test
    public void parse_unicodeIsNormalized() {
        assertParseSuccess(parser, " 1 m/Ｗｅａｋ", command(1, "Weak"));
        assertParseSuccess(parser, " １ m/x²", command(1, "x2"));
    }

    @Test
    public void parse_lengthBoundaries() {
        assertParseSuccess(parser, " 1 m/" + "a".repeat(500), command(1, "a".repeat(500)));
        assertParseFailure(parser, " 1 m/" + "a".repeat(501), AcademicNote.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidNote_failure() {
        for (String value : new String[] {"", "   ", "tab\there", "line\nbreak", "bell\u0007", "zero​width"}) {
            assertParseFailure(parser, " 1 m/" + value, AcademicNote.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_missingPrefix_failure() {
        assertParseFailure(parser, "", MISSING_NOTE);
        assertParseFailure(parser, "   ", MISSING_NOTE);
        assertParseFailure(parser, " 1", MISSING_NOTE);
        assertParseFailure(parser, " 1   ", MISSING_NOTE);
        assertParseFailure(parser, " 1m/note", MISSING_NOTE);
    }

    @Test
    public void parse_unexpectedTextBeforeThePrefix_failure() {
        assertParseFailure(parser, " 1 foo m/note", UNEXPECTED_TEXT);
        assertParseFailure(parser, " 1 foo", UNEXPECTED_TEXT);
        assertParseFailure(parser, " 1 2 m/note", UNEXPECTED_TEXT);
    }

    @Test
    public void parse_otherPrefixBeforeThePrefix_isAnUnknownParameter() {
        assertParseFailure(parser, " 1 x/y m/note", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, " 1 n/Weak", "Error: Unknown parameter: n/.");
        assertParseFailure(parser, " 1 M/note", "Error: Unknown parameter: M/.");
        assertParseFailure(parser, " x/1 m/note", "Error: Unknown parameter: x/.");
    }

    @Test
    public void parse_missingOrInvalidIndex_failure() {
        assertParseFailure(parser, " m/note", INVALID_INDEX);
        assertParseFailure(parser, "m/note", INVALID_INDEX);
        for (String index : new String[] {"0", "-1", "01", "00", "abc", "1.5", "+1", "1e2", "1/2"}) {
            assertParseFailure(parser, " " + index + " m/note", INVALID_INDEX);
        }
    }

    @Test
    public void parse_veryLargeIndex_isSyntacticallyValid() throws Exception {
        String huge = "9".repeat(100);
        assertParseSuccess(parser, " " + huge + " m/note", new NoteCommand(ParserUtil.parseIndex(huge),
                new AcademicNote("note")));
    }

    @Test
    public void parse_errorPrecedence() {
        // Unexpected text and unknown parameters come before the index and the note.
        assertParseFailure(parser, " 0 foo m/", UNEXPECTED_TEXT);
        assertParseFailure(parser, " abc x/1 m/ok", "Error: Unknown parameter: x/.");
        // The index syntax comes before the note value.
        assertParseFailure(parser, " 0 m/", INVALID_INDEX);
        assertParseFailure(parser, " abc m/" + "a".repeat(501), INVALID_INDEX);
        // The note value comes before a missing row, which is only known when the command runs.
        assertParseFailure(parser, " 99 m/", AcademicNote.MESSAGE_CONSTRAINTS);
    }
}
