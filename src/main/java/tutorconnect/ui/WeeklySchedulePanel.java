package tutorconnect.ui;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
    private final FilteredList<Student> visibleSchedule;
    private final Label heading = new Label("Weekly schedule");
    private final Label emptyMessage = new Label("No weekly lessons scheduled.");
    /**
     * Creates a summary without a separate selection or calendar state.
     */
    public WeeklySchedulePanel(ObservableList<Student> schedule) {
        visibleSchedule = new FilteredList<>(schedule);
        setId("weeklySchedule");
        setSpacing(8);
        setPadding(new Insets(12));
        getStyleClass().add("weekly-schedule");
        heading.setId("timetableHeading");
        heading.getStyleClass().add("cell_big_label");
        heading.getStyleClass().add("timetable-heading");
        emptyMessage.getStyleClass().add("timetable-empty");
        ListView<Student> rows = new ListView<>(visibleSchedule);
        rows.setId("weeklyScheduleList");
        rows.setPlaceholder(emptyMessage);
        rows.setFocusTraversable(false);
        rows.setSelectionModel(null);
        rows.setCellFactory(list -> new ScheduleCell());
        VBox.setVgrow(rows, Priority.ALWAYS);
        getChildren().addAll(heading, rows);
    }

    /** Filters only the existing sorted schedule; student numbering and selection are unaffected. */
    public void showDay(Optional<DayOfWeek> day) {
        visibleSchedule.setPredicate(student -> day.isEmpty()
                || WeeklySlotField.get(student).orElseThrow().getDay() == day.get());
        String name = day.map(value -> value.getDisplayName(TextStyle.FULL, Locale.ENGLISH)).orElse("");
        heading.setText(day.isPresent() ? name + " schedule" : "Weekly schedule");
        emptyMessage.setText(day.isPresent() ? "No lessons scheduled for " + name + "."
                : "No weekly lessons scheduled.");
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
