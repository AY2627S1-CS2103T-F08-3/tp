package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

public class StudentTagIntegrityTest {
    @Test
    public void constructor_nullTag_rejectsBeforeStudentCanBePublished() {
        var tags = new HashSet<>(ALICE.getTags());
        tags.add(null);
        assertThrows(NullPointerException.class, () -> new Student(ALICE.getId(), ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), ALICE.getAddress(), tags, ALICE.getStudentFields()));
    }

    @Test
    public void withDetails_nullTag_rejectsWithoutChangingOriginalStudent() {
        var tags = new HashSet<>(ALICE.getTags());
        tags.add(null);
        assertThrows(NullPointerException.class, () -> ALICE.withDetails(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), ALICE.getAddress(), tags));
        assertFalse(ALICE.getTags().contains(null));
    }

    @Test
    public void constructor_detachesTagsFromMutableInput() {
        var tags = new HashSet<>(ALICE.getTags());
        Student copy = ALICE.withDetails(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getAddress(), tags);
        tags.clear();
        assertEquals(ALICE.getTags(), copy.getTags());
    }
}
