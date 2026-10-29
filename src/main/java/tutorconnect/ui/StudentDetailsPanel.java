package tutorconnect.ui;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import tutorconnect.model.student.AcademicNote;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.Subject;
import tutorconnect.model.student.WeeklySlotField;

/** Read-only details for the selected student. Optional field owners can extend their value formatting here. */
public class StudentDetailsPanel extends VBox {
    static final String EMPTY_PROMPT = "Select a student to view details.";
    private static final String EMPTY_HINT = "Type a command that targets a student, such as note 1 m/..., "
            + "or press Ctrl/⌘ ↓ to move through the list.";
    private Student shownStudent;
    /** Creates an initially empty details panel. */
    public StudentDetailsPanel() {
        setId("studentDetails");
        setSpacing(12);
        setPadding(new Insets(4, 4, 12, 4));
        getStyleClass().add("student-details");
        showStudent(null);
    }

    /** Renders one complete published student, or an empty selection prompt. */
    public void showStudent(Student student) {
        if (student == shownStudent && !getChildren().isEmpty()) {
            return;
        }
        shownStudent = student;
        getChildren().clear();
        if (student == null) {
            getChildren().add(DetailRow.emptyState(EMPTY_PROMPT, EMPTY_HINT));
            return;
        }
        String level = EducationLevel.display(student.getStudentFields());
        String subject = Subject.display(student.getStudentFields());
        String name = student.getName().toString();
        getChildren().add(DetailRow.header("Name: " + name, name, subtitle(level, subject)));
        getChildren().add(DetailRow.field("Phone: " + student.getPhone(), "CONTACT"));
        getChildren().add(DetailRow.field("Address: " + student.getAddress(), null));
        getChildren().add(DetailRow.field(
                "Guardian phone: " + student.getStudentFields().display("guardianPhone"), null));
        getChildren().add(DetailRow.field("Education level: " + level, "TUTORING"));
        getChildren().add(DetailRow.field("Subject: " + subject, null));
        getChildren().add(DetailRow.field("Note: " + AcademicNote.display(student.getStudentFields()), null));
        getChildren().add(DetailRow.field(
                "Hourly rate: " + student.getHourlyRate().map(rate -> "S$" + rate).orElse("—"), null));
        getChildren().add(DetailRow.field("Weekly slot: " + WeeklySlotField.display(student), null));
        addRecordLines(AssessmentHistory.detailLines(student.getStudentFields()), "RECORDS");
        addRecordLines(PaymentHistory.detailLines(student.getStudentFields()), "PAYMENTS");
    }

    private static String subtitle(String level, String subject) {
        boolean hasLevel = !level.equals("—");
        boolean hasSubject = !subject.equals("—");
        if (hasLevel && hasSubject) {
            return level + "  ·  " + subject;
        }
        return hasLevel ? level : hasSubject ? subject : "No level or subject yet";
    }

    /** Adds a record list: its first line opens the section, and later lines are its items. */
    private void addRecordLines(List<String> lines, String section) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i > 0) {
                getChildren().add(DetailRow.item(line));
            } else if (line.endsWith(":")) {
                getChildren().add(DetailRow.caption(line, section));
            } else {
                getChildren().add(DetailRow.field(line, section));
            }
        }
    }
}
