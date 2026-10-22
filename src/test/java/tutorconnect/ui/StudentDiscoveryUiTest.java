package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.Student;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;

/** Exercises the command box and rendered row numbering on a desktop or virtual display. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
class StudentDiscoveryUiTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    void discovery_renumbersRowsAndPreservesSelectionOnErrors() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
            LogicManager logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, storage.getStudentDirectoryFilePath());
                window.fillInnerParts();
                window.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                @SuppressWarnings("unchecked")
                ListView<Student> rows = (ListView<Student>) stage.getScene().lookup("#studentListView");
                enter(input, "add n/Zoe p/81234567 a/Road");
                enter(input, "add n/Jose Tan p/81234568 a/Road");
                enter(input, "level 2 l/p1");
                enter(input, "add n/Amy Tan p/81234569 a/Road");
                enter(input, "level 3 l/p1");
                enter(input, "filter l/p1");
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                assertEquals(List.of("Jose Tan", "Amy Tan"),
                        rows.getItems().stream().map(student -> student.getName().fullName).toList());
                assertEquals(List.of("1. ", "2. "), rows.lookupAll("#id").stream()
                        .map(node -> ((Label) node).getText()).sorted().toList());
                rows.getSelectionModel().select(0);
                var selected = rows.getSelectionModel().getSelectedItem().getId();
                enter(input, "filter l/p1 l/p2");
                assertEquals(selected, rows.getSelectionModel().getSelectedItem().getId());
                enter(input, "level 1 l/p2");
                assertEquals("Amy Tan", rows.getItems().getFirst().getName().fullName);
                assertNull(rows.getSelectionModel().getSelectedItem());
                assertNull(logic.getSelectedStudentId());
                enter(input, "find os");
                assertEquals(selected, rows.getItems().getFirst().getId());
                enter(input, "find nobody");
                assertEquals(0, rows.getItems().size());
                assertEquals("No students found.", ((Label) rows.getPlaceholder()).getText());
                enter(input, "list");
                assertEquals(3, rows.getItems().size());
            } finally {
                stage.hide();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }
}
