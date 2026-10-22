package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;
import tutorconnect.testutil.StudentBuilder;

/** Real command-box and details acceptance for monthly payments; opt in with F01_UI_TESTS=true. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
public class PaymentUiTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void paymentCommand_refreshesDetailsAndListCommandShowsRecordedStatus() throws Exception {
        onFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addStudent(new StudentBuilder().withName("Alex Tan").withPhone("91234567").build());
            JsonStudentDirectoryStorage studentStorage = new JsonStudentDirectoryStorage(
                    directory.resolve("students.json"));
            LogicManager logic = new LogicManager(model, new StorageManager(studentStorage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, logic, studentStorage.getStudentDirectoryFilePath());
            window.fillInnerParts();
            window.show();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            try {
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea status = (TextArea) stage.getScene().lookup("#resultDisplay");
                enter(input, "payment 1 m/2026-10 s/pending");
                assertEquals("Payment status set for Alex Tan (+6591234567) (2026-10): pending.", status.getText());
                assertTrue(detailLabels(stage).contains("2026-10: pending"));

                enter(input, "payments m/2026-10");
                assertEquals("Alex Tan (+6591234567) — 2026-10: pending", status.getText());
                assertTrue(detailLabels(stage).contains("2026-10: pending"));
            } finally {
                stage.hide();
            }
            return null;
        });
    }

    private List<String> detailLabels(Stage stage) {
        StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
        return details.getChildren().stream().map(node -> ((Label) node).getText()).toList();
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new javafx.event.ActionEvent());
    }

    private void onFxThread(Callable<Void> action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action);
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }
}
