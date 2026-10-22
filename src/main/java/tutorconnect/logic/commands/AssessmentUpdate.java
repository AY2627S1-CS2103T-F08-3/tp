package tutorconnect.logic.commands;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;

/** Shared reading and rewriting of a student's assessment history by the assessment commands. */
final class AssessmentUpdate {
    private AssessmentUpdate() {}

    /**
     * Returns the student's history, empty if unset. Unreadable stored data is reported instead of being replaced,
     * so a hand-edited history is never overwritten by accident.
     */
    static AssessmentHistory historyOf(Student student) throws CommandException {
        try {
            return AssessmentHistory.of(student.getStudentFields());
        } catch (IllegalArgumentException e) {
            throw new CommandException(String.format(AssessmentHistory.MESSAGE_UNREADABLE, student.getName()), e);
        }
    }

    /** Returns the student with this history, retaining identity and unrelated fields; an empty one is removed. */
    static Student withHistory(Student student, AssessmentHistory history) {
        StudentFields fields = student.getStudentFields();
        if (!history.isEmpty()) {
            return student.withStudentFields(fields.with(AssessmentHistory.FIELD, history));
        }
        Map<String, JsonNode> stored = fields.toStorage();
        stored.remove(AssessmentHistory.FIELD.key());
        return student.withStudentFields(new StudentFields(stored));
    }
}
