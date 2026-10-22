package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class AssessmentRecordTest {

    private static String errorOf(Executable action) {
        return assertThrows(IllegalArgumentException.class, action).getMessage();
    }

    private static void assertInvalidDate(String input) {
        assertEquals(AssessmentRecord.MESSAGE_INVALID_DATE, errorOf(() -> AssessmentRecord.parseDate(input)),
                "[" + input + "]");
    }

    private static void assertInvalidTitle(String title) {
        assertEquals(AssessmentRecord.MESSAGE_INVALID_TITLE,
                errorOf(() -> new AssessmentRecord(LocalDate.of(2026, 10, 1), title, "A1")), "[" + title + "]");
    }

    private static void assertInvalidGrade(String grade) {
        assertEquals(AssessmentRecord.MESSAGE_INVALID_GRADE,
                errorOf(() -> new AssessmentRecord(LocalDate.of(2026, 10, 1), "Test", grade)), "[" + grade + "]");
    }

    @Test
    public void parseDate_realZeroPaddedDates_areAccepted() {
        assertEquals(LocalDate.of(2026, 10, 31), AssessmentRecord.parseDate("2026-10-31"));
        assertEquals(LocalDate.of(2028, 2, 29), AssessmentRecord.parseDate("2028-02-29"));
        assertEquals(LocalDate.of(2026, 1, 1), AssessmentRecord.parseDate("2026-01-01"));
        assertEquals(LocalDate.of(2026, 12, 31), AssessmentRecord.parseDate(" 2026-12-31 "));
        assertEquals(LocalDate.of(2026, 10, 31),
                AssessmentRecord.parseDate("２０２６－１０－３１"));
    }

    @Test
    public void parseDate_impossibleOrMisshapenDates_areRejected() {
        for (String input : List.of("", " ", "2026-02-30", "2026-02-29", "2026-13-01", "2026-00-10", "2026-10-32",
                "2026-10-00", "2026-04-31", "2026-2-3", "2026-02-3", "26-10-31", "20261031", "2026/10/31",
                "31-10-2026", "2026-10-31 10:00", "2026-10-31T10:00", "2026-10", "abcd-ef-gh", "2026-10-31x",
                "+2026-10-31", "2026-10-31\n", "2026-10-​31")) {
            assertInvalidDate(input);
        }
    }

    @Test
    public void title_acceptedCharactersAndLengths() {
        for (String title : List.of("Math Midterm", "P6 English (Oral) / Writing", "Paper 1: Algebra, Part #2",
                "O'Level Chemistry - Practical", "A", "1", "Maths & Science 2.0", "数学测验")) {
            assertEquals(title, new AssessmentRecord(LocalDate.of(2026, 10, 1), title, "A1").getTitle(), title);
        }
        assertEquals(60, new AssessmentRecord(LocalDate.of(2026, 10, 1), "a".repeat(60), "A1").getTitle().length());
        assertInvalidTitle("a".repeat(61));
    }

    @Test
    public void title_unsupportedOrEmptyValues_areRejected() {
        for (String title : List.of("", " ", "   ", "///", "- -", "(), .", "Math!", "Math_Test", "Math@Home",
                "100%", "Math+", "<b>Math</b>", "Math\tTest", "Math\nTest", "Math\u0000", "Math​Test")) {
            assertInvalidTitle(title);
        }
    }

    @Test
    public void grade_acceptedFormatsAndLengths() {
        for (String grade : List.of("A1", "A", "B+", "A-", "85/100", "72.5%", "Pass", "7 / 10", "100%", "9",
                "Distinction", "优")) {
            assertEquals(grade, new AssessmentRecord(LocalDate.of(2026, 10, 1), "Test", grade).getGrade(), grade);
        }
        assertEquals(20, new AssessmentRecord(LocalDate.of(2026, 10, 1), "Test", "a".repeat(20)).getGrade().length());
        assertInvalidGrade("a".repeat(21));
    }

    @Test
    public void grade_unsupportedOrEmptyValues_areRejected() {
        for (String grade : List.of("", " ", "///", "+-", "...", "A*", "85,5", "A_1", "A!", "(A)", "85#", "A:",
                "A\t1", "A\n1", "A\u0000", "A​1")) {
            assertInvalidGrade(grade);
        }
    }

    @Test
    public void text_isNormalizedAndKeepsItsCase() {
        AssessmentRecord record = new AssessmentRecord(LocalDate.of(2026, 10, 1), "  Math   Midterm  ",
                "  85 /  100 ");
        assertEquals("Math Midterm", record.getTitle());
        assertEquals("85 / 100", record.getGrade());
        assertEquals("a1", new AssessmentRecord(LocalDate.of(2026, 10, 1), "Test", "a1").getGrade());
        assertEquals("A1", new AssessmentRecord(LocalDate.of(2026, 10, 1), "Test", "Ａ１").getGrade());
    }

    @Test
    public void of_validatesTheDateThenTheTitleThenTheGrade() {
        assertEquals(AssessmentRecord.MESSAGE_INVALID_DATE, errorOf(() -> AssessmentRecord.of("bad", "@", "!")));
        assertEquals(AssessmentRecord.MESSAGE_INVALID_TITLE,
                errorOf(() -> AssessmentRecord.of("2026-10-01", "@", "!")));
        assertEquals(AssessmentRecord.MESSAGE_INVALID_GRADE,
                errorOf(() -> AssessmentRecord.of("2026-10-01", "Test", "!")));
    }

    @Test
    public void sameAssessment_sharesADateAndATitleIgnoringCaseAndSpacing() {
        AssessmentRecord record = AssessmentRecord.of("2026-10-01", "Math Midterm", "85/100");
        assertTrue(record.isSameAssessment(AssessmentRecord.of("2026-10-01", "  math   MIDTERM ", "A1")));
        assertFalse(record.isSameAssessment(AssessmentRecord.of("2026-10-02", "Math Midterm", "85/100")));
        assertFalse(record.isSameAssessment(AssessmentRecord.of("2026-10-01", "Math Final", "85/100")));
        assertFalse(record.isSameAssessment(null));
    }

    @Test
    public void equalsHashCodeAndToString() {
        AssessmentRecord record = AssessmentRecord.of("2026-10-01", "Math Midterm", "85/100");
        assertEquals(record, AssessmentRecord.of("2026-10-01", " Math  Midterm", "85/100"));
        assertEquals(record.hashCode(), AssessmentRecord.of("2026-10-01", "Math Midterm", "85/100").hashCode());
        assertNotEquals(record, AssessmentRecord.of("2026-10-01", "Math Midterm", "86/100"));
        assertNotEquals(record, AssessmentRecord.of("2026-10-01", "math midterm", "85/100"));
        assertNotEquals(record, AssessmentRecord.of("2026-10-02", "Math Midterm", "85/100"));
        assertEquals("2026-10-01 Math Midterm: 85/100", record.toString());
    }
}
