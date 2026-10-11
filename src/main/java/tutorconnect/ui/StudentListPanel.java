package tutorconnect.ui;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import tutorconnect.commons.core.LogsCenter;
import tutorconnect.model.student.Student;

/**
 * Panel containing the list of students.
 */
public class StudentListPanel extends UiPart<Region> {
    private static final String FXML = "StudentListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(StudentListPanel.class);

    @FXML
    private ListView<Student> studentListView;

    /**
     * Creates a {@code StudentListPanel} with the given {@code ObservableList}.
     */
    public StudentListPanel(ObservableList<Student> studentList) {
        this(studentList, unused -> {});
    }

    /** Passes row selection to the shared model/details view without interpreting displayed indices. */
    public StudentListPanel(ObservableList<Student> studentList, Consumer<Student> selectionHandler) {
        super(FXML);
        studentListView.setItems(studentList);
        studentListView.setPlaceholder(new Label("No students found."));
        studentListView.setCellFactory(listView -> new StudentListViewCell());
        studentListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                selectionHandler.accept(newValue));
    }

    /** Selects and scrolls to a stable student ID after a successful command publication. */
    public void selectStudent(UUID id) {
        studentListView.refresh();
        if (id == null) {
            studentListView.getSelectionModel().clearSelection();
            return;
        }
        for (int i = 0; i < studentListView.getItems().size(); i++) {
            if (studentListView.getItems().get(i).getId().equals(id)) {
                if (studentListView.getSelectionModel().getSelectedItem() == studentListView.getItems().get(i)) {
                    return;
                }
                studentListView.getSelectionModel().select(i);
                studentListView.scrollTo(i);
                return;
            }
        }
    }

    /** Selects and scrolls to a zero-based row in the currently displayed list. */
    public void selectIndex(int zeroBasedIndex) {
        studentListView.getSelectionModel().select(zeroBasedIndex);
        studentListView.scrollTo(zeroBasedIndex);
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Student} using a {@code StudentCard}.
     */
    class StudentListViewCell extends ListCell<Student> {
        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);

            if (empty || student == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new StudentCard(student, getIndex() + 1).getRoot());
            }
        }
    }

}
