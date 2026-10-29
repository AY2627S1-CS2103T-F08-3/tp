package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.Student;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;
import tutorconnect.testutil.StudentBuilder;

/** Exercises malformed and valid edits through the actual command box and persisted register. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
public class EditStudentUiTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void edit_filteredStudent_rejectsUnknownPrefixAndPublishesValidChange() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            Student hidden = new StudentBuilder().withName("Alex Tan").build();
            Student target = new StudentBuilder().withName("Nur Aisyah").build();
            model.addStudent(hidden);
            model.addStudent(target);
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
            storage.saveStudentDirectory(model.getStudentDirectory());
            LogicManager logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, storage.getStudentDirectoryFilePath());
                window.fillInnerParts();
                window.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea status = (TextArea) stage.getScene().lookup("#resultDisplay");
                @SuppressWarnings("unchecked")
                ListView<Student> rows = (ListView<Student>) stage.getScene().lookup("#studentListView");
                StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
                enter(input, "find Nur");
                rows.getSelectionModel().select(0);
                String saved = Files.readString(storage.getStudentDirectoryFilePath());
                var predicate = model.getStudentPredicate();
                List<String> detailText = details.getChildren().stream()
                        .map(node -> ((Label) node).getText()).toList();
                enter(input, "edit 1 a/21 Clementi x/accidental");
                assertEquals("Error: Unknown parameter: x/.", status.getText());
                assertEquals(saved, Files.readString(storage.getStudentDirectoryFilePath()));
                assertEquals(List.of(target), rows.getItems());
                assertSame(predicate, model.getStudentPredicate());
                assertEquals(target, rows.getSelectionModel().getSelectedItem());
                assertEquals(detailText, details.getChildren().stream().map(node -> ((Label) node).getText()).toList());

                enter(input, "edit 1 a/12/3 New Street");
                assertEquals("Student updated: Nur Aisyah.", status.getText());
                assertEquals(1, rows.getSelectionModel().getSelectedIndex());
                assertEquals(target.getId(), model.getSelectedStudentId());
                assertSame(hidden, rows.getItems().getFirst());
                assertTrue(details.getChildren().stream().map(node -> ((Label) node).getText())
                        .anyMatch(text -> text.equals("Address: 12/3 New Street")));
                Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getLast();
                assertEquals(target.getId(), reloaded.getId());
                assertEquals("12/3 New Street", reloaded.getAddress().value);
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
