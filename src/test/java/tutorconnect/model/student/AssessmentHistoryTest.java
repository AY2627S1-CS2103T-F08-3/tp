package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class AssessmentHistoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static AssessmentRecord record(String date, String title, String grade) {
        return AssessmentRecord.of(date, title, grade);
    }

    private static AssessmentHistory historyOf(AssessmentRecord... records) {
        AssessmentHistory history = new AssessmentHistory();
        for (AssessmentRecord record : records) {
            history = history.with(record);
        }
        return history;
    }

    private static JsonNode json(String text) throws Exception {
        return MAPPER.readTree(text);
    }

    private static void assertUnreadable(String stored) throws Exception {
        JsonNode node = json(stored);
        assertThrows(IllegalArgumentException.class, () -> AssessmentHistory.FIELD.decode(node), stored);
    }

    @Test
    public void records_areOrderedByDateAndTiesKeepTheOrderTheyWereAdded() {
        AssessmentHistory history = historyOf(record("2026-10-05", "C test", "A"), record("2026-09-01", "A test", "B"),
                record("2026-10-05", "D test", "C"), record("2026-09-01", "B test", "D"),
                record("2026-11-01", "E test", "E"));

        List<String> titles = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            titles.add(history.get(i).getTitle());
        }
        assertEquals(List.of("A test", "B test", "C test", "D test", "E test"), titles);
    }

    @Test
    public void numberIfAdded_isThePositionAfterEveryRecordOnOrBeforeThatDate() {
        AssessmentHistory history = historyOf(record("2026-09-01", "A", "1"), record("2026-10-05", "B", "2"));
        assertEquals(1, history.numberIfAdded(record("2026-08-01", "Early", "1")));
        assertEquals(2, history.numberIfAdded(record("2026-09-01", "Same day", "1")));
        assertEquals(2, history.numberIfAdded(record("2026-09-15", "Between", "1")));
        assertEquals(3, history.numberIfAdded(record("2026-10-05", "Same last day", "1")));
        assertEquals(3, history.numberIfAdded(record("2027-01-01", "Late", "1")));
        assertEquals(1, new AssessmentHistory().numberIfAdded(record("2026-01-01", "First", "1")));
    }

    @Test
    public void with_rejectsADuplicateAssessmentWhateverItsGrade() {
        AssessmentHistory history = historyOf(record("2026-10-01", "Math Midterm", "85/100"));
        assertTrue(history.findSameAssessment(record("2026-10-01", "  math midterm ", "A1")).isPresent());
        assertThrows(IllegalArgumentException.class, () -> history.with(record("2026-10-01", "MATH MIDTERM", "A1")));
        assertEquals(2, history.with(record("2026-10-02", "Math Midterm", "A1")).size());
        assertEquals(2, history.with(record("2026-10-01", "Math Final", "A1")).size());
    }

    @Test
    public void with_rejectsMoreThanTheMaximumNumberOfRecords() {
        AssessmentHistory history = new AssessmentHistory();
        for (int i = 1; i <= AssessmentHistory.MAX_RECORDS; i++) {
            assertFalse(history.isFull());
            history = history.with(record("2026-01-01", "Test " + i, "A"));
        }
        assertTrue(history.isFull());
        assertEquals(100, history.size());
        AssessmentHistory full = history;
        assertThrows(IllegalArgumentException.class, () -> full.with(record("2026-02-01", "One more", "A")));
    }

    @Test
    public void without_removesOnlyTheRecordAtThatPosition() {
        AssessmentHistory history = historyOf(record("2026-09-01", "A", "1"), record("2026-10-01", "B", "2"),
                record("2026-11-01", "C", "3"));

        AssessmentHistory smaller = history.without(1);

        assertEquals(2, smaller.size());
        assertEquals("A", smaller.get(0).getTitle());
        assertEquals("C", smaller.get(1).getTitle());
        assertEquals(3, history.size());
        assertTrue(smaller.without(1).without(0).isEmpty());
    }

    @Test
    public void storageCodec_roundTripsInDisplayOrder() throws Exception {
        AssessmentHistory history = historyOf(record("2026-10-05", "Later", "B+"),
                record("2026-09-01", "Earlier: Paper 1 / 2", "85/100"));

        JsonNode encoded = AssessmentHistory.FIELD.encode(history);

        assertEquals("assessments", AssessmentHistory.FIELD.key());
        assertEquals(json("[{\"date\":\"2026-09-01\",\"title\":\"Earlier: Paper 1 / 2\",\"grade\":\"85/100\"},"
                + "{\"date\":\"2026-10-05\",\"title\":\"Later\",\"grade\":\"B+\"}]"), encoded);
        assertEquals(history, AssessmentHistory.FIELD.decode(encoded));
    }

    @Test
    public void storageCodec_sortsAHandEditedUnorderedListKeepingTiesInFileOrder() throws Exception {
        AssessmentHistory decoded = AssessmentHistory.FIELD.decode(json(
                "[{\"date\":\"2026-10-05\",\"title\":\"B\",\"grade\":\"1\"},"
                        + "{\"date\":\"2026-09-01\",\"title\":\"A\",\"grade\":\"1\"},"
                        + "{\"date\":\"2026-10-05\",\"title\":\"C\",\"grade\":\"1\"}]"));

        assertEquals("A", decoded.get(0).getTitle());
        assertEquals("B", decoded.get(1).getTitle());
        assertEquals("C", decoded.get(2).getTitle());
    }

    @Test
    public void storageCodec_anEmptyArrayIsAnEmptyHistory() throws Exception {
        assertTrue(AssessmentHistory.FIELD.decode(json("[]")).isEmpty());
    }

    @Test
    public void storageCodec_unreadableStoredValues_areRejected() throws Exception {
        for (String stored : List.of("\"text\"", "5", "{}", "{\"date\":\"2026-10-01\"}", "[1]", "[\"x\"]", "[null]",
                "[[]]", "[{}]", "[{\"date\":\"2026-10-01\",\"title\":\"T\"}]",
                "[{\"date\":\"2026-10-01\",\"title\":\"T\",\"grade\":\"A\",\"extra\":\"x\"}]",
                "[{\"date\":\"2026-10-01\",\"title\":\"T\",\"grade\":5}]",
                "[{\"date\":20261001,\"title\":\"T\",\"grade\":\"A\"}]",
                "[{\"date\":null,\"title\":\"T\",\"grade\":\"A\"}]",
                "[{\"date\":\"2026-02-30\",\"title\":\"T\",\"grade\":\"A\"}]",
                "[{\"date\":\"2026-10-01\",\"title\":\"@\",\"grade\":\"A\"}]",
                "[{\"date\":\"2026-10-01\",\"title\":\"T\",\"grade\":\"!\"}]",
                "[{\"date\":\"2026-10-01\",\"title\":\"T\",\"grade\":\"A\"},"
                        + "{\"date\":\"2026-10-01\",\"title\":\" t \",\"grade\":\"B\"}]")) {
            assertUnreadable(stored);
        }
        StringBuilder tooMany = new StringBuilder("[");
        for (int i = 0; i <= AssessmentHistory.MAX_RECORDS; i++) {
            tooMany.append(i == 0 ? "" : ",")
                    .append("{\"date\":\"2026-10-01\",\"title\":\"Test ").append(i).append("\",\"grade\":\"A\"}");
        }
        assertUnreadable(tooMany.append("]").toString());
    }

    @Test
    public void of_readsTheEnvelopeOrAnEmptyHistory() throws Exception {
        assertTrue(AssessmentHistory.of(new StudentFields()).isEmpty());
        StudentFields stored = new StudentFields(Map.of("assessments",
                json("[{\"date\":\"2026-10-01\",\"title\":\"T\",\"grade\":\"A\"}]")));
        assertEquals(1, AssessmentHistory.of(stored).size());
        StudentFields unreadable = new StudentFields(Map.of("assessments", TextNode.valueOf("junk")));
        assertThrows(IllegalArgumentException.class, () -> AssessmentHistory.of(unreadable));
    }

    @Test
    public void detailLines_showTheCountAndNumberedRecordsOrAnEmptyState() throws Exception {
        assertEquals(List.of("Assessments: none recorded"), AssessmentHistory.detailLines(new StudentFields()));
        assertEquals(List.of("Assessments: none recorded"), AssessmentHistory.detailLines(
                new StudentFields(Map.of("assessments", json("[]")))));
        StudentFields stored = new StudentFields().with(AssessmentHistory.FIELD, historyOf(
                record("2026-10-05", "Later", "B+"), record("2026-09-01", "Earlier", "85/100")));
        assertEquals(List.of("Assessments (2):", "1. 2026-09-01 Earlier: 85/100", "2. 2026-10-05 Later: B+"),
                AssessmentHistory.detailLines(stored));
        assertEquals(List.of("Assessments: stored data could not be read"), AssessmentHistory.detailLines(
                new StudentFields(Map.of("assessments", IntNode.valueOf(5)))));
    }

    @Test
    public void equalsHashCodeAndToString() {
        AssessmentHistory one = historyOf(record("2026-10-01", "A", "1"));
        assertEquals(one, historyOf(record("2026-10-01", "A", "1")));
        assertEquals(one.hashCode(), historyOf(record("2026-10-01", "A", "1")).hashCode());
        assertFalse(one.equals(historyOf(record("2026-10-01", "A", "2"))));
        assertFalse(one.equals(null));
        assertEquals("[2026-10-01 A: 1]", one.toString());
    }
}
