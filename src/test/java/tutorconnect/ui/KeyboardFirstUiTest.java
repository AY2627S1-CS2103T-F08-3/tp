package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Student;
import tutorconnect.model.util.SampleDataUtil;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;

/** Drives the real window with key events only, as a type-first user would. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
public class KeyboardFirstUiTest {
    private static final boolean IS_MAC = System.getProperty("os.name").startsWith("Mac");

    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void commandBar_historyCompletionHintsAndEscape() throws Exception {
        onFxThread(stage -> {
            TextField input = (TextField) stage.getScene().lookup("#commandTextField");
            Label hint = (Label) stage.getScene().lookup("#commandHint");
            assertEquals(input, stage.getScene().getFocusOwner());
            assertEquals(CommandBox.IDLE_HINT, hint.getText());

            input.setText("sch");
            assertEquals("schedule INDEX d/DAY t/TIME   (Tab to complete)", hint.getText());
            press(input, KeyCode.TAB, false);
            assertEquals("schedule ", input.getText());
            assertEquals("schedule INDEX d/DAY t/TIME", hint.getText());
            assertEquals(input, stage.getScene().getFocusOwner());

            input.setText("zzz");
            assertTrue(hint.getText().startsWith("No command starts with \"zzz\""));

            enter(input, "find alex");
            enter(input, "delete 0");
            TextArea result = (TextArea) stage.getScene().lookup("#resultDisplay");
            assertTrue(result.getStyleClass().contains(ResultDisplay.ERROR_STYLE_CLASS));
            input.setText("draft");
            press(input, KeyCode.UP, false);
            assertEquals("delete 0", input.getText());
            press(input, KeyCode.UP, false);
            assertEquals("find alex", input.getText());
            press(input, KeyCode.DOWN, false);
            press(input, KeyCode.DOWN, false);
            assertEquals("draft", input.getText());
            press(input, KeyCode.ESCAPE, false);
            assertEquals("", input.getText());
            enter(input, "list");
            assertFalse(result.getStyleClass().contains(ResultDisplay.ERROR_STYLE_CLASS));
        });
    }

    @Test
    public void window_shortcutsSelectStudentsAndRouteTypingToCommandBar() throws Exception {
        onFxThread(stage -> {
            TextField input = (TextField) stage.getScene().lookup("#commandTextField");
            @SuppressWarnings("unchecked")
            ListView<Student> rows = (ListView<Student>) stage.getScene().lookup("#studentListView");
            StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
            assertEquals(List.of(StudentDetailsPanel.EMPTY_PROMPT), texts(details));

            press(input, KeyCode.DOWN, true);
            assertEquals(0, rows.getSelectionModel().getSelectedIndex());
            assertEquals("Name: Alex Yeoh", texts(details).get(0));
            press(input, KeyCode.DOWN, true);
            press(input, KeyCode.UP, true);
            press(input, KeyCode.UP, true);
            assertEquals(0, rows.getSelectionModel().getSelectedIndex());
            assertEquals("", input.getText());

            rows.requestFocus();
            rows.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, "l", "", KeyCode.UNDEFINED, false, false, false, false));
            assertEquals(input, stage.getScene().getFocusOwner());
            assertEquals("l", input.getText());
            rows.requestFocus();
            press(rows, KeyCode.L, true);
            assertEquals(input, stage.getScene().getFocusOwner());

            assertEquals("06", ((Label) stage.getScene().lookup("#shownCount")).getText());
            enter(input, "find alex");
            assertEquals("01", ((Label) stage.getScene().lookup("#shownCount")).getText());
            assertEquals("(1)", ((Label) stage.getScene().lookup("#listCountLabel")).getText());
            assertNull(rows.getSelectionModel().getSelectedItem());
        });
    }

    private void onFxThread(UiCheck check) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager(SampleDataUtil.getSampleStudentDirectory(), new UserPrefs());
            var logic = new LogicManager(model, new StorageManager(
                    new JsonStudentDirectoryStorage(directory.resolve("students.json")),
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, directory.resolve("students.json"));
                window.show();
                window.fillInnerParts();
                check.run(stage);
            } finally {
                stage.hide();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private static void press(Node target, KeyCode code, boolean isShortcut) {
        target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, isShortcut && !IS_MAC, false,
                isShortcut && IS_MAC));
    }

    private static void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }

    private static List<String> texts(StudentDetailsPanel details) {
        return details.getChildren().stream().map(node -> ((Label) node).getText()).toList();
    }

    @FunctionalInterface
    private interface UiCheck {
        void run(Stage stage) throws Exception;
    }
}
