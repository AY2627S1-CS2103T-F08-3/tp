package seedu.address.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import seedu.address.model.person.Person;
import seedu.address.model.person.WeeklySlotField;

/** Read-only details for the selected student. Optional field owners can extend their value formatting here. */
public class StudentDetailsPanel extends VBox {
    private Person shownStudent;
    /** Creates an initially empty details panel. */
    public StudentDetailsPanel() {
        setId("studentDetails");
        setSpacing(12);
        setPadding(new Insets(16));
        showStudent(null);
    }

    /** Renders one complete published student, or an empty selection prompt. */
    public void showStudent(Person student) {
        if (student == shownStudent && !getChildren().isEmpty()) {
            return;
        }
        shownStudent = student;
        getChildren().clear();
        if (student == null) {
            addDetail("Select a student to view details.");
            return;
        }
        addDetail("Name: " + student.getName());
        addDetail("Phone: " + student.getPhone());
        addDetail("Address: " + student.getAddress());
        addDetail("Guardian phone: " + student.getStudentFields().display("guardianPhone"));
        addDetail("Education level: " + student.getStudentFields().display("educationLevel"));
        addDetail("Subject: " + student.getStudentFields().display("subject"));
        addDetail("Hourly rate: " + student.getStudentFields().display("hourlyRate"));
        addDetail("Weekly slot: " + WeeklySlotField.display(student));
    }

    private void addDetail(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("cell_small_label");
        getChildren().add(label);
    }
}
