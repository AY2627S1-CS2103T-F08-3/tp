package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class SubjectDisplayTest {
    @Test
    public void display_validStoredText_normalizesWithoutChangingStorage() {
        StudentFields fields = new StudentFields(Map.of("subject", TextNode.valueOf("  Ｈ２   Math  ")));
        assertEquals("H2 Math", Subject.display(fields));
        assertEquals("  Ｈ２   Math  ", fields.toStorage().get("subject").asText());
    }

    @Test
    public void display_missingNullOrUnreadableSubject_usesEmDash() {
        assertEquals("—", Subject.display(new StudentFields()));
        for (JsonNode value : List.of(NullNode.getInstance(), IntNode.valueOf(12), TextNode.valueOf("@@"))) {
            assertEquals("—", Subject.display(new StudentFields(Map.of("subject", value))));
        }
    }

    @Test
    public void display_assignmentAndReplacement_preserveCapitalization() {
        StudentFields fields = new StudentFields().with(Subject.FIELD, new Subject("H2 Math"));
        assertEquals("H2 Math", Subject.display(fields));
        assertEquals("h2 maths", Subject.display(fields.with(Subject.FIELD, new Subject("h2 maths"))));
        assertEquals("H2 Math", Subject.display(fields));
    }
}
