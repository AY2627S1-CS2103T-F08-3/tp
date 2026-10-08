package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class SubjectTest {

    private static void assertInvalid(String input) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new Subject(input));
        assertEquals(Subject.MESSAGE_CONSTRAINTS, error.getMessage(), "[" + input + "]");
    }

    @Test
    public void constructor_documentedExamplesAndAllowedSymbols_areAccepted() {
        for (String input : List.of("O-Level Chemistry", "H2 Math", "Primary 6 English", "Math/Science",
                "Arts & Crafts", "Children's Literature", "Physics (Advanced)", "Chinese 华文", "数学",
                "A1", "Ab", "Math 2", "English (Oral) / Writing & Grammar - Sec 3")) {
            assertEquals(input, new Subject(input).value, input);
        }
    }

    @Test
    public void constructor_lengthBoundaries_countAfterNormalization() {
        assertInvalid("A");
        assertInvalid(" A ");
        assertEquals("Ab", new Subject("Ab").value);
        assertEquals(50, new Subject("a".repeat(50)).value.length());
        assertInvalid("a".repeat(51));
        assertEquals("a b", new Subject("a    b").value);
        assertEquals(50, new Subject("a".repeat(24) + "          " + "a".repeat(25)).value.length());
        // Length counts Unicode code points: 50 supplementary letters (100 UTF-16 units) fit, 51 do not.
        String supplementaryLetter = "𠀀";
        assertEquals(supplementaryLetter.repeat(50), new Subject(supplementaryLetter.repeat(50)).value);
        assertInvalid(supplementaryLetter.repeat(51));
    }

    @Test
    public void constructor_requiresAtLeastOneLetter() {
        for (String input : List.of("12", "1 2", "&&", "- -", "(1)", "1/2")) {
            assertInvalid(input);
        }
    }

    @Test
    public void constructor_unsupportedCharacters_areRejected() {
        for (String input : List.of("Math@Home", "Math!", "Math.", "Math,Science", "Math#1", "Math_1", "Math+",
                "Math:", "Math;", "Math\"", "<b>Math</b>", "Math\tScience", "Math\nScience", "Math\u0000",
                "Math​", "", " ", " Math")) {
            assertInvalid(input);
        }
    }

    @Test
    public void constructor_normalizesSpacingAndKeepsDisplayCase() {
        assertEquals("O-Level Chemistry", new Subject("  O-Level    Chemistry  ").value);
        assertEquals("H2 MATH", new Subject("H2 MATH").value);
        assertEquals("Math", new Subject("Ｍａｔｈ").value); // full-width letters via NFKC
    }

    @Test
    public void equals_ignoresCaseAndSpacingButNotContent() {
        Subject subject = new Subject("H2 Math");
        assertEquals(subject, new Subject("h2 math"));
        assertEquals(subject, new Subject("  H2    MATH "));
        assertEquals(subject.hashCode(), new Subject("h2 MATH").hashCode());
        assertNotEquals(subject, new Subject("H1 Math"));
        assertNotEquals(subject, new Subject("H2Math"));
        assertEquals("H2 Math", subject.toString());
    }

    @Test
    public void storageCodec_keepsDisplayValue() {
        Subject subject = new Subject("O-Level Chemistry");
        assertEquals("subject", Subject.FIELD.key());
        assertEquals(TextNode.valueOf("O-Level Chemistry"), Subject.FIELD.encode(subject));
        assertEquals(subject, Subject.FIELD.decode(TextNode.valueOf("O-Level Chemistry")));
        assertThrows(IllegalArgumentException.class, () -> Subject.FIELD.decode(IntNode.valueOf(7)));
        assertThrows(IllegalArgumentException.class, () -> Subject.FIELD.decode(TextNode.valueOf("@@")));
    }
}
