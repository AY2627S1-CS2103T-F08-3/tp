package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.Assert.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.testutil.StudentBuilder;

class StudentCriteriaPredicateTest {
    private final Student student = new StudentBuilder().build().withStudentFields(new StudentFields()
            .with(EducationLevel.FIELD, new EducationLevel("s4"))
            .with(Subject.FIELD, new Subject("H2 Math")));

    @Test
    void test_optionalCriteria_useFullLabelsAndAndMatching() {
        assertTrue(predicate("sec4", null).test(student));
        assertTrue(predicate(null, " ｈ２   MATH ").test(student));
        assertTrue(predicate("secondary 4", "H2 Math").test(student));
        assertFalse(predicate("p4", "H2 Math").test(student));
        assertFalse(predicate("s4", "Math").test(student));
        assertFalse(predicate(null, "Math").test(student));
    }

    @Test
    void test_unsetOrUnreadableCriteria_doNotMatch() {
        Student unset = new StudentBuilder().build();
        assertFalse(predicate("s4", null).test(unset));
        assertFalse(predicate(null, "Math").test(unset));
        Student legacy = student.withStudentFields(new StudentFields(Map.of(
                "educationLevel", TextNode.valueOf("invalid"), "subject", TextNode.valueOf("H2 Math"))));
        assertFalse(predicate("s4", null).test(legacy));
        assertTrue(predicate(null, "H2 Math").test(legacy));
        assertThrows(IllegalArgumentException.class, () -> predicate(null, null));
    }

    private StudentCriteriaPredicate predicate(String level, String subject) {
        return new StudentCriteriaPredicate(Optional.ofNullable(level).map(EducationLevel::new),
                Optional.ofNullable(subject).map(Subject::new));
    }
}
