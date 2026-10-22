package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import tutorconnect.commons.util.AppUtil;

/**
 * A student's assessment records, ordered by date and, for the same date, in the order they were added. A
 * record's number is its 1-based position in that order. The history never holds two records that are the same
 * assessment and never holds more than {@value #MAX_RECORDS} records.
 */
public final class AssessmentHistory {

    public static final int MAX_RECORDS = 100;
    public static final String MESSAGE_UNREADABLE =
            "Error: The stored assessment history for %1$s could not be read. "
                    + "Correct or remove the \"assessments\" entry in the data file.";
    public static final String MESSAGE_UNREADABLE_DISPLAY = "Assessments: stored data could not be read";
    public static final String EMPTY_DISPLAY = "Assessments: none recorded";

    private static final String STORED_MESSAGE = "Assessment history must be a list of date, title and grade records.";
    private static final Set<String> RECORD_KEYS = Set.of("date", "title", "grade");

    public static final StudentField<AssessmentHistory> FIELD = new StudentField<>() {
        @Override
        public String key() {
            return "assessments";
        }

        @Override
        public AssessmentHistory decode(JsonNode value) {
            AppUtil.checkArgument(value.isArray(), STORED_MESSAGE);
            List<AssessmentRecord> decoded = new ArrayList<>();
            for (JsonNode element : value) {
                decoded.add(decodeRecord(element));
            }
            return new AssessmentHistory(decoded);
        }

        @Override
        public JsonNode encode(AssessmentHistory value) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            for (AssessmentRecord record : value.records) {
                array.add(JsonNodeFactory.instance.objectNode().put("date", record.getDate().toString())
                        .put("title", record.getTitle()).put("grade", record.getGrade()));
            }
            return array;
        }
    };

    private final List<AssessmentRecord> records;

    /** Creates an empty history. */
    public AssessmentHistory() {
        this(List.of());
    }

    /**
     * Creates a history from records in any order. Records are put in date order, keeping the given order for the
     * same date.
     *
     * @throws IllegalArgumentException if there are too many records or two are the same assessment.
     */
    public AssessmentHistory(List<AssessmentRecord> records) {
        requireNonNull(records);
        AppUtil.checkArgument(records.size() <= MAX_RECORDS, STORED_MESSAGE);
        List<AssessmentRecord> ordered = new ArrayList<>(records);
        ordered.sort(Comparator.comparing(AssessmentRecord::getDate));
        for (int i = 0; i < ordered.size(); i++) {
            for (int j = 0; j < i; j++) {
                AppUtil.checkArgument(!ordered.get(i).isSameAssessment(ordered.get(j)), STORED_MESSAGE);
            }
        }
        this.records = Collections.unmodifiableList(ordered);
    }

    private static AssessmentRecord decodeRecord(JsonNode element) {
        AppUtil.checkArgument(element != null && element.isObject() && element.size() == RECORD_KEYS.size(),
                STORED_MESSAGE);
        Iterator<String> names = element.fieldNames();
        while (names.hasNext()) {
            AppUtil.checkArgument(RECORD_KEYS.contains(names.next()), STORED_MESSAGE);
        }
        for (String key : RECORD_KEYS) {
            AppUtil.checkArgument(element.get(key).isTextual(), STORED_MESSAGE);
        }
        return AssessmentRecord.of(element.get("date").asText(), element.get("title").asText(),
                element.get("grade").asText());
    }

    /** Returns the stored history, empty if unset. */
    public static AssessmentHistory of(StudentFields fields) {
        return fields.get(FIELD).orElseGet(AssessmentHistory::new);
    }

    /** Returns the lines to show in the details panel: a count and one numbered line per record, or an empty state. */
    public static List<String> detailLines(StudentFields fields) {
        Optional<AssessmentHistory> history;
        try {
            history = fields.get(FIELD);
        } catch (IllegalArgumentException e) {
            return List.of(MESSAGE_UNREADABLE_DISPLAY);
        }
        if (history.isEmpty() || history.get().isEmpty()) {
            return List.of(EMPTY_DISPLAY);
        }
        List<String> lines = new ArrayList<>();
        lines.add("Assessments (" + history.get().size() + "):");
        for (int i = 0; i < history.get().size(); i++) {
            lines.add((i + 1) + ". " + history.get().get(i));
        }
        return lines;
    }

    public int size() {
        return records.size();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public boolean isFull() {
        return records.size() >= MAX_RECORDS;
    }

    /** Returns the record at the given zero-based position. */
    public AssessmentRecord get(int zeroBased) {
        return records.get(zeroBased);
    }

    /** Returns the stored record that is the same assessment as {@code record}, if any. */
    public Optional<AssessmentRecord> findSameAssessment(AssessmentRecord record) {
        return records.stream().filter(existing -> existing.isSameAssessment(record)).findFirst();
    }

    /** Returns the 1-based number the record would get if it were added now. */
    public int numberIfAdded(AssessmentRecord record) {
        LocalDate date = record.getDate();
        return (int) records.stream().filter(existing -> !existing.getDate().isAfter(date)).count() + 1;
    }

    /**
     * Returns a history that also holds {@code record}.
     *
     * @throws IllegalArgumentException if the history is full or already holds the same assessment.
     */
    public AssessmentHistory with(AssessmentRecord record) {
        requireNonNull(record);
        AppUtil.checkArgument(!isFull() && findSameAssessment(record).isEmpty(), STORED_MESSAGE);
        List<AssessmentRecord> added = new ArrayList<>(records);
        added.add(record);
        return new AssessmentHistory(added);
    }

    /** Returns a history without the record at the given zero-based position. */
    public AssessmentHistory without(int zeroBased) {
        List<AssessmentRecord> remaining = new ArrayList<>(records);
        remaining.remove(zeroBased);
        return new AssessmentHistory(remaining);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof AssessmentHistory otherHistory && records.equals(otherHistory.records);
    }

    @Override
    public int hashCode() {
        return records.hashCode();
    }

    @Override
    public String toString() {
        return records.toString();
    }
}
