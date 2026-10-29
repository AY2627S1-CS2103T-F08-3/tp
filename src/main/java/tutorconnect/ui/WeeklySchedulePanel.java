package tutorconnect.ui;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tutorconnect.commons.core.LogsCenter;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;

/**
 * A lightweight, read-only weekly lesson summary observing the independently sorted model list.
 */
public class WeeklySchedulePanel extends VBox {
    private static final Logger logger = LogsCenter.getLogger(WeeklySchedulePanel.class);
    /** Width taken by the day/time block and spacing beside each name. */
    private static final double SLOT_BLOCK_WIDTH = 96;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final FilteredList<Student> visibleSchedule;
    private final Label heading = new Label("Weekly schedule");
    private final Label emptyMessage = new Label("No weekly lessons scheduled.");
    private final ListView<Student> rows;
    private int firstShownRow;
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
        rows = new ListView<>(visibleSchedule);
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
        logger.fine("Showing " + heading.getText() + " with " + visibleSchedule.size() + " lesson(s)");
    }

    /** Returns the timetable's list, the node that receives keyboard focus for this panel. */
    ListView<Student> getRowsView() {
        return rows;
    }

    /** Scrolls the timetable by {@code delta} rows; used for keyboard scrolling because rows are not selectable. */
    void scrollBy(int delta) {
        if (visibleSchedule.isEmpty()) {
            return;
        }
        firstShownRow = Math.max(0, Math.min(visibleSchedule.size() - 1, firstShownRow + delta));
        rows.scrollTo(firstShownRow);
    }

    /**
     * Displays the full weekday and HH:mm alongside the student's name.
     */
    public static String formatEntry(Student student) {
        return WeeklySlotField.get(student).orElseThrow() + " — " + student.getName();
    }

    private static class ScheduleCell extends ListCell<Student> {
        private final Label day = new Label();
        private final Label time = new Label();
        private final Label name = new Label();
        private final HBox row;

        ScheduleCell() {
            setWrapText(true);
            // Let the virtualized list size rows to its viewport, not the unwrapped text.
            setPrefWidth(0);
            day.getStyleClass().add("slot-day");
            time.getStyleClass().add("slot-time");
            name.getStyleClass().add("slot-name");
            name.setWrapText(true);
            VBox slot = new VBox(day, time);
            slot.getStyleClass().add("slot-block");
            slot.setMinWidth(Region.USE_PREF_SIZE);
            row = new HBox(12, slot, name);
            row.setAlignment(Pos.CENTER_LEFT);
            name.maxWidthProperty().bind(widthProperty().subtract(SLOT_BLOCK_WIDTH));
        }

        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);
            setText(null);
            if (empty || student == null) {
                setGraphic(null);
                return;
            }
            WeeklySlot slot = WeeklySlotField.get(student).orElseThrow();
            day.setText(slot.getDay().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT));
            time.setText(slot.getTime().format(TIME_FORMAT));
            name.setText(student.getName().toString());
            setAccessibleText(formatEntry(student));
            setGraphic(row);
        }
    }
}
