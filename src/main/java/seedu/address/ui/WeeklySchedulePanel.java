package seedu.address.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import seedu.address.model.person.Person;
import seedu.address.model.person.WeeklySlotField;

/**
 * A lightweight, read-only weekly lesson summary observing the independently sorted model list.
 */
public class WeeklySchedulePanel extends VBox {
    /**
     * Creates a summary without a separate selection or calendar state.
     */
    public WeeklySchedulePanel(ObservableList<Person> schedule) {
        setId("weeklySchedule");
        setSpacing(8);
        setPadding(new Insets(12));
        getStyleClass().add("weekly-schedule");
        Label heading = new Label("Weekly schedule");
        heading.getStyleClass().add("cell_big_label");
        ListView<Person> rows = new ListView<>(schedule);
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
    public static String formatEntry(Person person) {
        return WeeklySlotField.get(person).orElseThrow() + " — " + person.getName();
    }

    private static class ScheduleCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);
            setText(empty || person == null ? null : formatEntry(person));
        }
    }
}
