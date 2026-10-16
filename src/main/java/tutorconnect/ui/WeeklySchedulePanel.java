package tutorconnect.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlotField;

/**
 * A lightweight, read-only weekly lesson summary observing the independently sorted model list.
 */
public class WeeklySchedulePanel extends VBox {
    /**
     * Creates a summary without a separate selection or calendar state.
     */
    public WeeklySchedulePanel(ObservableList<Student> schedule) {
        setId("weeklySchedule");
        setSpacing(8);
        setPadding(new Insets(12));
        getStyleClass().add("weekly-schedule");
        Label heading = new Label("Weekly schedule");
        heading.getStyleClass().add("cell_big_label");
        ListView<Student> rows = new ListView<>(schedule);
        rows.setId("weeklyScheduleList");
        rows.setPlaceholder(new Label("No weekly lessons scheduled."));
        rows.setFocusTraversable(false);
        rows.setSelectionModel(null);
        rows.setCellFactory(list -> new ScheduleCell());
        VBox.setVgrow(rows, Priority.ALWAYS);
        getChildren().addAll(heading, rows);
    }

    /**
     * Displays the full weekday and HH:mm alongside the student's name.
     */
    public static String formatEntry(Student student) {
        return WeeklySlotField.get(student).orElseThrow() + " — " + student.getName();
    }

    private static class ScheduleCell extends ListCell<Student> {
        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);
            setText(empty || student == null ? null : formatEntry(student));
        }
    }
}
