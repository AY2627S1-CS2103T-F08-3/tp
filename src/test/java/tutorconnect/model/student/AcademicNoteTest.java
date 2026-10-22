package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class AcademicNoteTest {

    private static final String EM_DASH = "—";

    private static void assertInvalid(String input) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new AcademicNote(input));
        assertEquals(AcademicNote.MESSAGE_CONSTRAINTS, error.getMessage(), "[" + input + "]");
    }

    @Test
    public void constructor_sensibleNotesAndPunctuation_arePreservedAsTyped() {
        for (String note : List.of("Weak in calculus", "short attention span",
                "Weak at fractions; needs 2x practice (see p.4) - 50% done!",
                "Prefers visuals and/or diagrams, he/she w/ parent, speed in m/s",
                "Parent's email: mum@example.com #urgent $5 + 3 = 8 * 2",
                "\"quoted\" & <tagged> [bracketed] {braced} 100% ~ ^ | _",
                "数学需要加强", "Great job 😀", "A", "ALL CAPS and lower case")) {
            assertEquals(note, new AcademicNote(note).value, note);
        }
    }

    @Test
    public void constructor_lengthBoundaries_countUnicodeCodePoints() {
        assertEquals("a", new AcademicNote("a").value);
        assertEquals(500, new AcademicNote("a".repeat(500)).value.length());
        assertInvalid("a".repeat(501));
        String supplementaryLetter = "𠀀";
        assertEquals(supplementaryLetter.repeat(500), new AcademicNote(supplementaryLetter.repeat(500)).value);
        assertInvalid(supplementaryLetter.repeat(501));
        // Spaces are collapsed before the length is measured.
        assertEquals("a b", new AcademicNote("a" + " ".repeat(600) + "b").value);
    }

    @Test
    public void constructor_emptyOrBlank_isRejected() {
        for (String input : List.of("", " ", "   ", " ", "　", "  ")) {
            assertInvalid(input);
        }
    }

    @Test
    public void constructor_controlFormatAndLineBreakCharacters_areRejected() {
        for (String prohibited : List.of("\t", "\r", "\n", "\u0000", "\u0007", "\u001b", "\u007f", "​",
                " ", " ", "‮")) {
            assertInvalid(prohibited);
            assertInvalid(prohibited + "note");
            assertInvalid("note" + prohibited);
            assertInvalid("some" + prohibited + "note");
        }
    }

    @Test
    public void constructor_normalizesUnicodeAndSpacing() {
        assertEquals("Weak in calculus", new AcademicNote("  Weak   in calculus  ").value);
        assertEquals("Weak in calculus", new AcademicNote("Weak in　calculus").value);
        assertEquals("A1", new AcademicNote("Ａ１").value);
        assertEquals("first", new AcademicNote("ﬁrst").value);
        // Compatibility characters are normalized (NFKC), e.g. a superscript two becomes a plain digit.
        assertEquals("x2", new AcademicNote("x²").value);
    }

    @Test
    public void equals_isExactSoACaseOnlyChangeIsADifferentNote() {
        AcademicNote note = new AcademicNote("Weak in calculus");
        assertEquals(note, new AcademicNote("  Weak   in calculus "));
        assertEquals(note.hashCode(), new AcademicNote("Weak in calculus").hashCode());
        assertNotEquals(note, new AcademicNote("weak in calculus"));
        assertNotEquals(note, new AcademicNote("Weak in calculus."));
        assertEquals("Weak in calculus", note.toString());
    }

    @Test
    public void storageCodec_roundTripsAsAText() {
        AcademicNote note = new AcademicNote("he/she and/or w/ parent");
        assertEquals("academicNote", AcademicNote.FIELD.key());
        assertEquals(TextNode.valueOf("he/she and/or w/ parent"), AcademicNote.FIELD.encode(note));
        assertEquals(note, AcademicNote.FIELD.decode(TextNode.valueOf("he/she and/or w/ parent")));
        for (var unreadable : List.of(IntNode.valueOf(7), TextNode.valueOf(""), TextNode.valueOf("   "),
                TextNode.valueOf("a".repeat(501)), TextNode.valueOf("tab\there"))) {
            assertThrows(IllegalArgumentException.class, () -> AcademicNote.FIELD.decode(unreadable));
        }
    }

    @Test
    public void display_showsTheNoteOrAnEmDash() {
        assertEquals(EM_DASH, AcademicNote.display(new StudentFields()));
        assertEquals(EM_DASH, AcademicNote.display(new StudentFields(Map.of("subject", TextNode.valueOf("Math")))));
        assertEquals("Weak in calculus", AcademicNote.display(new StudentFields().with(AcademicNote.FIELD,
                new AcademicNote("Weak in calculus"))));
        assertEquals(EM_DASH, AcademicNote.display(new StudentFields(Map.of("academicNote", IntNode.valueOf(3)))));
        assertEquals(EM_DASH, AcademicNote.display(new StudentFields(Map.of("academicNote",
                TextNode.valueOf("")))));
    }
}
