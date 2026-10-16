package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class EducationLevelDisplayTest {

    private static final String EM_DASH = "—";

    private static StudentFields stored(String text) {
        return new StudentFields(Map.of("educationLevel", TextNode.valueOf(text)));
    }

    @Test
    public void display_unsetLevel_isAnEmDash() {
        assertEquals(EM_DASH, EducationLevel.display(new StudentFields()));
        assertEquals(EM_DASH, EducationLevel.display(new StudentFields(Map.of("subject", TextNode.valueOf("Math")))));
    }

    @Test
    public void display_canonicalLevel_isShownAsIs() {
        assertEquals("Secondary 4", EducationLevel.display(new StudentFields().with(EducationLevel.FIELD,
                new EducationLevel("Sec4"))));
        assertEquals("Primary 6", EducationLevel.display(stored("Primary 6")));
        assertEquals("JC 2", EducationLevel.display(stored("JC 2")));
    }

    @Test
    public void display_storedAliasOrCase_isShownCanonically() {
        assertEquals("Secondary 4", EducationLevel.display(stored("sec4")));
        assertEquals("Secondary 4", EducationLevel.display(stored("S 4")));
        assertEquals("Secondary 4", EducationLevel.display(stored("SECONDARY 4")));
        assertEquals("Primary 1", EducationLevel.display(stored("p1")));
        assertEquals("JC 2", EducationLevel.display(stored("jc2")));
    }

    @Test
    public void display_unreadableStoredValue_isAnEmDashLikeTheLevelCommandTreatsIt() {
        for (String text : new String[] {"SECONDARY_1", "Sec 6", "Preschool", "", " "}) {
            assertEquals(EM_DASH, EducationLevel.display(stored(text)), "[" + text + "]");
        }
        assertEquals(EM_DASH, EducationLevel.display(new StudentFields(Map.of("educationLevel", IntNode.valueOf(4)))));
    }
}
