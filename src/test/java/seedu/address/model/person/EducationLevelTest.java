package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class EducationLevelTest {

    @Test
    public void constructor_everyCanonicalLevel_isAccepted() {
        for (int grade = 1; grade <= 6; grade++) {
            assertEquals("Primary " + grade, new EducationLevel("Primary " + grade).value);
        }
        for (int grade = 1; grade <= 5; grade++) {
            assertEquals("Secondary " + grade, new EducationLevel("Secondary " + grade).value);
        }
        for (int grade = 1; grade <= 2; grade++) {
            assertEquals("JC " + grade, new EducationLevel("JC " + grade).value);
        }
    }

    @Test
    public void constructor_everyAliasFamily_mapsToCanonicalName() {
        for (int grade = 1; grade <= 6; grade++) {
            assertEquals("Primary " + grade, new EducationLevel("P" + grade).value);
        }
        for (int grade = 1; grade <= 5; grade++) {
            assertEquals("Secondary " + grade, new EducationLevel("Sec" + grade).value);
            assertEquals("Secondary " + grade, new EducationLevel("S" + grade).value);
        }
        for (int grade = 1; grade <= 2; grade++) {
            assertEquals("JC " + grade, new EducationLevel("JC" + grade).value);
        }
    }

    @Test
    public void constructor_caseAndSpacingVariants_areEquivalent() {
        EducationLevel expected = new EducationLevel("Secondary 4");
        for (String input : List.of("Sec4", "Sec 4", "sec4", "SEC 4", "s4", "S 4", "secondary4", "SECONDARY 4",
                "  Sec   4  ", "Ｓｅｃ４")) {
            assertEquals(expected, new EducationLevel(input), input);
            assertEquals("Secondary 4", new EducationLevel(input).value, input);
        }
        assertEquals("Primary 6", new EducationLevel("p 6").value);
        assertEquals("JC 2", new EducationLevel("jc2").value);
    }

    @Test
    public void constructor_unsupportedLevels_areRejected() {
        for (String input : List.of("", " ", "Primary 0", "Primary 7", "P0", "P7", "Secondary 0", "Secondary 6",
                "Sec6", "S6", "S0", "JC 0", "JC 3", "JC3", "Sec Four", "Secondary Four", "Primary Six",
                "Pri 6", "Primary", "Sec", "JC", "Primary 06", "P 10", "Primary 1 2", "Preschool", "K2",
                "Polytechnic", "ITE", "IB", "University", "Year 4", "Secondary 4 Express", "P6A", "Sec-4",
                "Sec4\n", "Sec\u00004")) {
            assertInvalid(input);
        }
    }

    private static void assertInvalid(String input) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new EducationLevel(input));
        assertEquals(EducationLevel.MESSAGE_CONSTRAINTS, error.getMessage(), "[" + input + "]");
    }

    @Test
    public void equalsAndHashCode_useCanonicalName() {
        assertEquals(new EducationLevel("P1"), new EducationLevel("primary 1"));
        assertEquals(new EducationLevel("P1").hashCode(), new EducationLevel("Primary 1").hashCode());
        assertNotEquals(new EducationLevel("P1"), new EducationLevel("P2"));
        assertEquals("Secondary 4", new EducationLevel("sec4").toString());
    }

    @Test
    public void storageCodec_roundTripsAsCanonicalText() {
        EducationLevel level = new EducationLevel("Sec4");
        assertEquals("educationLevel", EducationLevel.FIELD.key());
        assertEquals(TextNode.valueOf("Secondary 4"), EducationLevel.FIELD.encode(level));
        assertEquals(level, EducationLevel.FIELD.decode(TextNode.valueOf("Secondary 4")));
        assertThrows(IllegalArgumentException.class, () -> EducationLevel.FIELD.decode(IntNode.valueOf(4)));
        TextNode legacyText = TextNode.valueOf("SECONDARY_4");
        assertThrows(IllegalArgumentException.class, () -> EducationLevel.FIELD.decode(legacyText));
    }
}
